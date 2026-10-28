package ma.emsi.tp1amal.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;

/**
 * Gère l'interface avec l'API de Gemini.
 * Son rôle est essentiellement de lancer une requête à chaque nouvelle
 * question qu'on veut envoyer à l'API.
 *
 * De portée dependent pour réinitialiser la conversation à chaque fois que
 * l'instance qui l'utilise est renouvelée.
 */
@Dependent
public class LlmClientPourGemini implements Serializable {

    // Clé pour l'API du LLM
    private final String key;

    // Client REST. Facilite les échanges avec une API REST.
    private Client clientRest;

    // Représente un endpoint de serveur REST
    private final WebTarget target;

    public LlmClientPourGemini() {

        // Récupère la clé secrète depuis la variable d'environnement Windows.
        this.key = System.getenv("GEMINI_KEY");

        if (this.key == null || this.key.isBlank()) {
            throw new IllegalStateException(
                    "La variable d'environnement GEMINI_KEY n'est pas définie."
            );
        }

        // Client REST pour envoyer des requêtes vers l'API.
        this.clientRest = ClientBuilder.newClient();

        // Endpoint de l'API Gemini.
        this.target = clientRest.target(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent"
        ).queryParam("key", this.key);
    }

    /**
     * Envoie une requête à l'API de Gemini.
     *
     * @param requestEntity le corps de la requête (en JSON).
     * @return réponse REST de l'API (corps en JSON).
     */
    public Response envoyerRequete(Entity requestEntity) {

        Invocation.Builder request =
                target.request(MediaType.APPLICATION_JSON_TYPE);

        // Envoie la requête POST au LLM.
        return request.post(requestEntity);
    }

    public void closeClient() {
        this.clientRest.close();
    }
}
