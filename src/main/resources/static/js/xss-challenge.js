// XSS Challenge JavaScript

// Charger les commentaires au chargement de la page
document.addEventListener('DOMContentLoaded', function() {
    loadComments();
});

// Variable pour tracker si l'admin a "visité"
let adminHasVisited = false;

// Fonction pour poster un commentaire
async function postComment() {
    const author = document.getElementById('authorName').value.trim();
    const comment = document.getElementById('commentText').value.trim();
    
    if (!author || !comment) {
        showResponse('postResponse', 'Veuillez remplir tous les champs.', 'error');
        return;
    }
    
    try {
        const response = await fetch('/api/xss/comments', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                author: author,
                comment: comment
            })
        });
        
        if (response.ok) {
            try {
                const result = await response.json();
                showResponse('postResponse', `✅ Commentaire posté avec succès par ${result.author}!`, 'success');
            } catch (jsonError) {
                showResponse('postResponse', `✅ Commentaire posté avec succès !`, 'success');
            }
            
            // Recharger les commentaires
            setTimeout(() => {
                loadComments();
            }, 1000);
        } else {
            showResponse('postResponse', `❌ Erreur HTTP: ${response.status}`, 'error');
        }
        
    } catch (error) {
        showResponse('postResponse', `❌ Erreur de connexion: ${error.message}`, 'error');
    }
}

// Fonction pour simuler la visite d'un administrateur
async function simulateAdminVisit() {
    try {
        const response = await fetch('/api/xss/admin/simulate-visit', {
            method: 'POST',
            credentials: 'include'
        });
        
        if (response.ok) {
            try {
                const result = await response.json();
                adminHasVisited = result.admin_visited || true;
                
                showResponse('simulationResponse', 
                    `✅ Visite admin simulée avec succès !<br>💡 L'admin a maintenant des cookies temporaires. Rechargez les commentaires pour déclencher votre XSS !`, 
                    'success');
            } catch (jsonError) {
                adminHasVisited = true;
                showResponse('simulationResponse', 
                    `✅ Visite admin simulée avec succès !<br>💡 L'admin a maintenant des cookies temporaires. Rechargez les commentaires pour déclencher votre XSS !`, 
                    'success');
            }
        } else {
            showResponse('simulationResponse', `❌ Erreur HTTP: ${response.status}`, 'error');
        }
        
    } catch (error) {
        showResponse('simulationResponse', `❌ Erreur de connexion: ${error.message}`, 'error');
    }
}

// Fonction pour afficher les cookies actuels (utile pour vérifier l'exfiltration)
function showCurrentCookies() {
    showResponse('cookieResponse', 
        `💡 Les cookies admin sont temporaires et n'apparaissent que pendant le chargement des commentaires !<br><br>
        📝 Utilisez un payload XSS pour les capturer`);
}

// Fonction pour charger et afficher les commentaires
async function loadComments() {
    // Si l'admin a visité, créer temporairement le cookie admin
    if (adminHasVisited) {
        // Créer le cookie temporaire avec le flag
        document.cookie = "admin_flag=FLAG{XSS_STORED_COOKIE_THEFT_SUCCESS}; path=/; max-age=10";
        console.log("🔥 Cookie admin temporaire créé pour XSS...");
    }
    
    try {
        const response = await fetch('/api/xss/comments');
        
        if (response.ok) {
            try {
                const result = await response.json();
                displayComments(result.comments);
                
                // Supprimer le cookie temporaire après affichage des commentaires (permet au XSS de s'exécuter d'abord)
                if (adminHasVisited) {
                    setTimeout(() => {
                        document.cookie = "admin_flag=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT";
                        console.log("🧹 Cookie admin temporaire supprimé");
                    }, 2000); // 2 secondes pour permettre au XSS de s'exécuter
                }
                
            } catch (jsonError) {
                showResponse('commentsContainer', '❌ Erreur lors du chargement des commentaires', 'error');
            }
        } else {
            showResponse('commentsContainer', `❌ Erreur HTTP: ${response.status}`, 'error');
        }
        
    } catch (error) {
        showResponse('commentsContainer', `❌ Erreur de connexion: ${error.message}`, 'error');
    }
}

// Fonction pour afficher les commentaires (VULNÉRABLE - pas d'échappement HTML)
function displayComments(comments) {
    const container = document.getElementById('commentsContainer');
    
    if (!comments || comments.length === 0) {
        container.innerHTML = '<p class="no-comments">Aucun commentaire trouvé.</p>';
        return;
    }
    
    let html = '<div class="comments-list">';
    
    comments.forEach(comment => {
        const date = new Date(comment.createdAt).toLocaleString('fr-FR');
        // VULNÉRABILITÉ XSS : innerHTML utilisé directement sans échappement
        html += `
            <div class="comment-item">
                <div class="comment-header">
                    <strong class="comment-author">${comment.author}</strong>
                    <span class="comment-date">${date}</span>
                </div>
                <div class="comment-content">
                    ${comment.comment}
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    
    // VULNÉRABILITÉ : Le contenu est injecté directement sans échappement
    container.innerHTML = html;
}

// Fonction utilitaire pour afficher les réponses
function showResponse(elementId, message, type) {
    const element = document.getElementById(elementId);
    element.innerHTML = message;
    element.className = `response-panel ${type}`;
    element.style.display = 'block';
    
    // Faire disparaître après 10 secondes pour les messages de succès
    if (type === 'success') {
        setTimeout(() => {
            element.style.display = 'none';
        }, 10000);
    }
}

