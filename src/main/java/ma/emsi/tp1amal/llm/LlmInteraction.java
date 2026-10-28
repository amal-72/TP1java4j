package ma.emsi.tp1amal.llm;

import java.io.Serializable;

public record LlmInteraction(
        String questionJson,
        String reponseJson,
        String reponseExtraite
) implements Serializable {
}
