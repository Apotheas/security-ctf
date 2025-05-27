// JWT Challenge JavaScript
async function login() {
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const responseDiv = document.getElementById('loginResponse');
    
    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });
        
        const data = await response.json();
        responseDiv.style.display = 'block';
        
        if (response.ok) {
            responseDiv.className = 'response-panel success';
            responseDiv.innerHTML = `
                <h3>✅ Connexion Réussie !</h3>
                <p><strong>Nom d'utilisateur :</strong> ${data.username}</p>
                <p><strong>Rôle :</strong> ${data.role}</p>
                <p><strong>Token JWT :</strong></p>
                <div class="code-block">${data.token}</div>
                <button class="btn btn-secondary" onclick="copyToken('${data.token}')">📋 Copier le Token</button>
            `;
        } else {
            responseDiv.className = 'response-panel error';
            responseDiv.innerHTML = `<h3>❌ Échec de la Connexion</h3><p>${data.error}</p>`;
        }
    } catch (error) {
        responseDiv.style.display = 'block';
        responseDiv.className = 'response-panel error';
        responseDiv.innerHTML = `<h3>❌ Erreur de Requête</h3><p>${error.message}</p>`;
    }
}

function copyToken(token) {
    navigator.clipboard.writeText(token);
    alert('Token copié dans le presse-papiers !');
}

async function accessAdminSecret() {
    const token = document.getElementById('adminToken').value;
    const responseDiv = document.getElementById('adminResponse');
    
    if (!token) {
        alert('Veuillez entrer un token forgé d\'abord !');
        return;
    }
    
    try {
        const response = await fetch(`/api/auth/admin/secret?token=${encodeURIComponent(token)}`);
        const data = await response.json();
        
        responseDiv.style.display = 'block';
        
        if (response.ok) {
            responseDiv.className = 'response-panel success';
            responseDiv.innerHTML = `
                <h3>🎉 SUCCÈS ! Flag Capturé !</h3>
                <div class="code-block">${data.flag}</div>`;
        } else {
            responseDiv.className = 'response-panel error';
            responseDiv.innerHTML = `<h3>❌ Accès Refusé</h3><p>${data.error}</p>`;
        }
    } catch (error) {
        responseDiv.style.display = 'block';
        responseDiv.className = 'response-panel error';
        responseDiv.innerHTML = `<h3>❌ Erreur de Requête</h3><p>${error.message}</p>`;
    }
} 