// Access Control Challenge JavaScript
let isAuthenticated = false;

async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const responseDiv = document.getElementById('loginResponse');
    
    try {
        const response = await fetch('/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: `username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`,
            credentials: 'include'
        });
        
        responseDiv.style.display = 'block';
        
        if (response.ok) {
            isAuthenticated = true;
            responseDiv.className = 'response-panel success';
            responseDiv.innerHTML = `
                <h3>✅ Connexion Réussie !</h3>
                <p><strong>Utilisateur :</strong> ${username}</p>
                <p>Redirection vers votre profil...</p>
            `;
            
            // Redirection vers l'endpoint du profil utilisateur
            setTimeout(() => {
                window.location.href = '/api/users/1/profile';
            }, 2000);
        } else {
            responseDiv.className = 'response-panel error';
            responseDiv.innerHTML = `<h3>❌ Échec de la Connexion</h3><p>Vérifiez vos identifiants.</p>`;
        }
    } catch (error) {
        responseDiv.style.display = 'block';
        responseDiv.className = 'response-panel error';
        responseDiv.innerHTML = `<h3>❌ Erreur de Requête</h3><p>${error.message}</p>`;
    }
} 