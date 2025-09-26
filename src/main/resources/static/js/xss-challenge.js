// XSS Challenge JavaScript - Version améliorée avec isolation par session

// Charger les commentaires au chargement de la page
document.addEventListener('DOMContentLoaded', function() {
    loadComments();
});

// Variables globales
let currentSessionId = null;

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
                currentSessionId = result.sessionId;
                
                showResponse('simulationResponse', 
                    ` Simulation admin démarrée !<br>
                     ${result.hint}<br>
                     Endpoint cookie admin: <code>${result.adminCookieEndpoint}</code><br>
                     Session: <code>${result.sessionId}</code>`, 
                    'success');
            } catch (jsonError) {
                showResponse('simulationResponse', 
                    ` Simulation admin démarrée !`, 
                    'success');
            }
        } else {
            showResponse('simulationResponse', `❌ Erreur HTTP: ${response.status}`, 'error');
        }
        
    } catch (error) {
        showResponse('simulationResponse', `❌ Erreur de connexion: ${error.message}`, 'error');
    }
}


// Fonction pour nettoyer les commentaires de l'utilisateur
async function cleanupComments() {
    try {
        const response = await fetch('/api/xss/comments/cleanup', {
            method: 'DELETE',
            credentials: 'include'
        });
        
        if (response.ok) {
            const result = await response.json();
            showResponse('cleanupResponse', 
                `✅ ${result.message}<br>
                🆔 Session: <code>${result.sessionId}</code>`, 
                'success');
            
            // Recharger les commentaires
            setTimeout(loadComments, 500);
        } else {
            const error = await response.json();
            showResponse('cleanupResponse', `❌ ${error.message}`, 'error');
        }
        
    } catch (error) {
        showResponse('cleanupResponse', `❌ Erreur de connexion: ${error.message}`, 'error');
    }
}

// Fonction pour charger et afficher les commentaires (avec simulation cookie admin)
async function loadComments() {
    try {
        const response = await fetch('/api/xss/comments', {
            credentials: 'include'
        });
        
        if (response.ok) {
            try {
                const result = await response.json();
                currentSessionId = result.sessionId;
                
                // Afficher si l'admin a visité
                if (result.adminVisited) {
                    console.log('🤖 Admin a visité cette session et a exécuté automatiquement tous les payloads XSS');
                }
                
                displayComments(result.comments);
                
                // Afficher les infos de session dans la console
                console.log(`📋 Commentaires chargés pour la session: ${result.sessionId}`);
                console.log(`📊 Total commentaires: ${result.totalComments}`);
                
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
        container.innerHTML = `
            <div class="no-comments" style="text-align: center; padding: 40px; color: #666;">
                <h3>💬 Aucun commentaire pour l'instant</h3>
                <p>Soyez le premier à poster un commentaire !</p>
                <p style="font-size: 12px; color: #999;">💡 Postez votre payload XSS ici pour commencer le défi</p>
            </div>
        `;
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

