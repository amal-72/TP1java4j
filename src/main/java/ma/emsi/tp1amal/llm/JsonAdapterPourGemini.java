package ma.emsi.tp1amal.llm;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.json.*;
import jakarta.json.stream.JsonGenerator;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe pour gérer le JSON des requêtes à l'API de Gemini.
 * Contient l'état JSON de la conversation et des méthodes pour manipuler le JSON.
 */
@Dependent
public class JsonAdapterPourGemini implements Serializable {

    private String systemRole;

    /**
     * Le "-" final indique que la valeur sera ajoutée à la fin du tableau.
     */
    private final JsonPointer pointer = Json.createPointer("/contents/-");

    /**
     * Requête JSON, à partir du prompt de l'utilisateur.
     */
    private JsonObject requeteJson;
    private String texteRequeteJson;

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    /**
     * Client pour envoyer une requête à l'API de Gemini.
     */
    @Inject
    private LlmClientPourGemini geminiClient;

    /**
     * Envoie une requête à l'API de Gemini.
     */
    public LlmInteraction envoyerRequete(String question) throws RequeteException {
        String requestBody;

        if (this.requeteJson == null) {
            // Première question : crée le JSON avec le rôle système.
            requestBody = creerRequeteJson(this.systemRole, question);
        } else {
            // Ajoute la nouvelle question à la conversation.
            requestBody = ajouteQuestionDansJsonRequete(question);
        }

        Entity<String> entity = Entity.entity(
                requestBody,
                MediaType.APPLICATION_JSON_TYPE
        );

        this.texteRequeteJson = prettyPrinting(this.requeteJson);

        try (Response response = geminiClient.envoyerRequete(entity)) {
            String texteReponseJson = response.readEntity(String.class);

            if (response.getStatus() == 200) {
                String reponseExtraite = extractReponse(texteReponseJson);

                return new LlmInteraction(
                        this.texteRequeteJson,
                        texteReponseJson,
                        reponseExtraite
                );
            } else {
                JsonObject objet = Json.createReader(
                        new StringReader(requestBody)
                ).readObject();

                throw new RequeteException(
                        response.getStatus() + " : " + response.getStatusInfo()
                                + " Réponse API : " + texteReponseJson,
                        prettyPrinting(objet)
                );
            }
        }
    }

    /**
     * Crée le JSON de la requête avec le rôle système et la première question.
     */
    private String creerRequeteJson(String systemRole, String question) {
        JsonArray systemInstructionParts = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("text", systemRole))
                .build();

        JsonObject systemInstruction = Json.createObjectBuilder()
                .add("parts", systemInstructionParts)
                .build();

        JsonArray userContentParts = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("text", question))
                .build();

        JsonObject userContent = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", userContentParts)
                .build();

        JsonArray contents = Json.createArrayBuilder()
                .add(userContent)
                .build();

        JsonObject rootJson = Json.createObjectBuilder()
                .add("system_instruction", systemInstruction)
                .add("contents", contents)
                .build();

        this.requeteJson = rootJson;

        return rootJson.toString();
    }

    /**
     * Ajoute une nouvelle question au JSON de la conversation.
     */
    private String ajouteQuestionDansJsonRequete(String nouvelleQuestion) {
        JsonObject nouveauMessageJson = Json.createObjectBuilder()
                .add("text", nouvelleQuestion)
                .build();

        JsonObjectBuilder newPartBuilder = Json.createObjectBuilder()
                .add("role", "user")
                .add("parts", Json.createArrayBuilder()
                        .add(nouveauMessageJson)
                        .build());

        this.requeteJson = this.pointer.add(
                this.requeteJson,
                newPartBuilder.build()
        );

        this.texteRequeteJson = prettyPrinting(this.requeteJson);

        return this.requeteJson.toString();
    }

    /**
     * Formate le JSON pour le rendre plus lisible.
     */
    private String prettyPrinting(JsonObject jsonObject) {
        Map<String, Boolean> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);

        JsonWriterFactory writerFactory =
                Json.createWriterFactory(config);

        StringWriter stringWriter = new StringWriter();

        try (JsonWriter jsonWriter =
                     writerFactory.createWriter(stringWriter)) {
            jsonWriter.write(jsonObject);
        }

        return stringWriter.toString();
    }

    /**
     * Extrait le texte de la réponse et ajoute le message du modèle
     * à l'historique JSON pour la prochaine requête.
     */
    private String extractReponse(String json) {
        try (JsonReader jsonReader =
                     Json.createReader(new StringReader(json))) {

            JsonObject jsonObject = jsonReader.readObject();

            JsonObject messageReponse = jsonObject
                    .getJsonArray("candidates")
                    .getJsonObject(0)
                    .getJsonObject("content");

            this.requeteJson = this.pointer.add(
                    this.requeteJson,
                    messageReponse
            );

            return messageReponse
                    .getJsonArray("parts")
                    .getJsonObject(0)
                    .getString("text");

        } catch (JsonException e) {
            throw new IllegalStateException(
                    "Impossible de lire la réponse JSON de Gemini",
                    e
            );
        }
    }
}
