// SQL Injection Challenge JavaScript
function setSearchValue(value) {
    document.getElementById('searchInput').value = value;
}

function searchProducts() {
    const searchTerm = document.getElementById('searchInput').value;
    const resultsDiv = document.getElementById('searchResults');
    
    if (!searchTerm.trim()) {
        resultsDiv.innerHTML = '<div class="error"><h3>Erreur :</h3><p>Veuillez entrer un terme de recherche</p></div>';
        return;
    }
    
    resultsDiv.innerHTML = '<div style="text-align: center; color: #0064c8; font-size: 1.2em;">🔍 Recherche en cours...</div>';
    
    fetch(`/api/injection/search?name=${encodeURIComponent(searchTerm)}`)
        .then(response => response.json())
        .then(data => {
            if (data.error) {
                resultsDiv.innerHTML = `
                    <div class="error">
                        <h3>Erreur SQL :</h3>
                        <p><strong>Erreur :</strong> ${data.error}</p>
                        ${data.details ? `<p><strong>Détails :</strong> ${data.details}</p>` : ''}
                        ${data.hint ? `<p><strong>Indice :</strong> ${data.hint}</p>` : ''}
                    </div>
                `;
            } else {
                let html = `<h3 style="color: #0064c8; margin-bottom: 20px; text-align: center;">
                    📦 ${data.totalResults} produit(s) trouvé(s) pour "${data.query}"
                </h3>`;
                
                if (data.products && data.products.length > 0) {
                    data.products.forEach(product => {
                        html += `
                            <div class="product">
                                <h3>${product.name}</h3>
                                <p>${product.description}</p>
                                <p><strong class="category">Catégorie :</strong> ${product.category}</p>
                                <p class="price">${product.price} €</p>
                            </div>
                        `;
                    });
                } else {
                    html += '<p style="text-align: center; color: #cccccc; font-size: 1.1em;">Aucun produit trouvé.</p>';
                }
                
                resultsDiv.innerHTML = html;
            }
        })
        .catch(error => {
            resultsDiv.innerHTML = `<div class="error"><h3>Erreur de Requête :</h3><p>${error.message}</p></div>`;
        });
}

// Permettre la recherche avec la touche Entrée
document.addEventListener('DOMContentLoaded', function() {
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                searchProducts();
            }
        });
    }
}); 