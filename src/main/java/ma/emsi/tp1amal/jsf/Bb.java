package ma.emsi.tp1amal.jsf;

import ma.emsi.tp1amal.llm.JsonAdapterPourGemini;
import ma.emsi.tp1amal.llm.LlmInteraction;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Named
@ViewScoped
public class Bb implements Serializable {

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private List<SelectItem> listeRolesSysteme;

    private String question;
    private String reponse;
    private StringBuilder conversation = new StringBuilder();

    private boolean debug;
    private String texteRequeteJson;
    private String texteReponseJson;

    @Inject
    private JsonAdapterPourGemini jsonAdapter;

    @Inject
    private FacesContext facesContext;

    public Bb() {
        this.roleSysteme = "You are a helpful assistant. You help the user to find the information they need. If the user type a question, you answer it.";
    }

    public String getRoleSysteme() {
        return this.roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return this.roleSystemeChangeable;
    }

    public String getQuestion() {
        return this.question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return this.reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public String getTexteRequeteJson() {
        return texteRequeteJson;
    }

    public void setTexteRequeteJson(String texteRequeteJson) {
        this.texteRequeteJson = texteRequeteJson;
    }

    public String getTexteReponseJson() {
        return texteReponseJson;
    }

    public void setTexteReponseJson(String texteReponseJson) {
        this.texteReponseJson = texteReponseJson;
    }

    public String envoyer() {

        if (this.question == null || this.question.isBlank()) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Texte question vide",
                    "Il manque le texte de la question"
            );
            facesContext.addMessage(null, message);
            return null;
        }

        try {
            jsonAdapter.setSystemRole(this.roleSysteme);

            LlmInteraction interaction =
                    jsonAdapter.envoyerRequete(this.question);

            this.reponse = interaction.reponseExtraite();
            this.texteRequeteJson = interaction.questionJson();
            this.texteReponseJson = interaction.reponseJson();

            if (this.conversation.isEmpty()) {
                this.roleSystemeChangeable = false;
            }

        } catch (Exception e) {
            this.reponse = "Erreur : " + e.getMessage();

            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Problème de connexion avec l'API du LLM",
                    "Problème de connexion avec l'API du LLM : "
                            + e.getMessage() + "\n"
                            + Arrays.toString(e.getStackTrace())
            );

            facesContext.addMessage(null, message);
        }

        afficherConversation();
        return null;
    }

    public String nouveauChat() {
        jsonAdapter.nouvelleConversation();

        this.question = null;
        this.reponse = null;
        this.conversation = new StringBuilder();
        this.roleSystemeChangeable = true;
        this.texteRequeteJson = null;
        this.texteReponseJson = null;

        return null;
    }

    private void afficherConversation() {
        this.conversation
                .append("== User:\n")
                .append(question)
                .append("\n== Serveur:\n")
                .append(reponse)
                .append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            this.listeRolesSysteme = new ArrayList<>();

            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            this.listeRolesSysteme.add(
                    new SelectItem(role, "Assistant")
            );

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(
                    new SelectItem(role, "Traducteur Anglais-Français")
            );

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    and tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(
                    new SelectItem(role, "Guide touristique")
            );

            // Nouveau rôle système : Traducteur d'émotions
            role = """
                    You are an emotional message translator.
                    You transform cold, awkward or simple messages into
                    natural, warm and expressive messages.
                    Always preserve the original meaning and adapt the tone
                    to the context.
                    Keep your responses short and authentic.
                    """;
            this.listeRolesSysteme.add(
                    new SelectItem(role, "Traducteur d'émotions")
            );
        }

        return this.listeRolesSysteme;
    }
}