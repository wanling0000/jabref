package org.jabref.logic.ai.chatting.model;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.ai.chatting.AiChatLogic;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.AiProvider;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.mistralai.MistralAiChatModel;

/**
 * Wrapper around langchain4j chat language model.
 * <p>
 * Notice, that the real chat model is created lazily, when it's needed. This is done, so API key is fetched only,
 * when user wants to chat with AI.
 */
public class JabRefChatLanguageModel implements ChatModel, AutoCloseable {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(5);

    private final AiPreferences aiPreferences;

    private final HttpClient httpClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("ai-api-connection-pool-%d").build()
    );

    private Optional<ChatModel> langchainChatModel = Optional.empty();

    public JabRefChatLanguageModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;
        this.httpClient = HttpClient.newBuilder().connectTimeout(CONNECTION_TIMEOUT).executor(executorService).build();

        setupListeningToPreferencesChanges();
    }

    /**
     * Update the underlying {@link dev.langchain4j.model.chat.ChatModel} by current {@link AiPreferences} parameters.
     * When the model is updated, the chat messages are not lost.
     * See {@link AiChatLogic}, where messages are stored in {@link ChatMemory},
     * and see {@link org.jabref.logic.ai.chatting.chathistory.ChatHistoryStorage}.
     */
    private void rebuild() {
        String apiKey = aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider());
        if (!aiPreferences.getEnableAi() || (apiKey.isEmpty() && aiPreferences.getAiProvider() != AiProvider.GPT4ALL)) {
            langchainChatModel = Optional.empty();
            return;
        }

        switch (aiPreferences.getAiProvider()) {
            case OPEN_AI -> langchainChatModel = Optional.of(new JvmOpenAiChatLanguageModel(aiPreferences, httpClient));

            case GPT4ALL-> langchainChatModel = Optional.of(new Gpt4AllModel(aiPreferences, httpClient));

            case MISTRAL_AI -> langchainChatModel = Optional.of(MistralAiChatModel
                    .builder()
                    .apiKey(apiKey)
                    .modelName(aiPreferences.getSelectedChatModel())
                    .temperature(aiPreferences.getTemperature())
                    .baseUrl(aiPreferences.getSelectedApiBaseUrl())
                    .logRequests(true)
                    .logResponses(true)
                    .build()
            );

            case GEMINI -> // NOTE: {@link GoogleAiGeminiChatModel} doesn't support API base url.
                    langchainChatModel = Optional.of(GoogleAiGeminiChatModel
                            .builder()
                            .apiKey(apiKey)
                            .modelName(aiPreferences.getSelectedChatModel())
                            .temperature(aiPreferences.getTemperature())
                            .logRequestsAndResponses(true)
                            .build()
                    );

            case HUGGING_FACE -> // NOTE: {@link HuggingFaceChatModel} doesn't support API base url.
                    langchainChatModel = Optional.of(HuggingFaceChatModel
                            .builder()
                            .accessToken(apiKey)
                            .modelId(aiPreferences.getSelectedChatModel())
                            .temperature(aiPreferences.getTemperature())
                            .timeout(Duration.ofMinutes(2))
                            .build()
                    );
        }
    }

    private void setupListeningToPreferencesChanges() {
        // Setting "langchainChatModel" to "Optional.empty()" will trigger a rebuild on the next usage

        aiPreferences.enableAiProperty().addListener(_ -> langchainChatModel = Optional.empty());
        aiPreferences.aiProviderProperty().addListener(_ -> langchainChatModel = Optional.empty());
        aiPreferences.customizeExpertSettingsProperty().addListener(_ -> langchainChatModel = Optional.empty());
        aiPreferences.temperatureProperty().addListener(_ -> langchainChatModel = Optional.empty());

        aiPreferences.addListenerToChatModels(() -> langchainChatModel = Optional.empty());
        aiPreferences.addListenerToApiBaseUrls(() -> langchainChatModel = Optional.empty());
        aiPreferences.setApiKeyChangeListener(() -> langchainChatModel = Optional.empty());
    }

    @Override
    public ChatResponse chat(List<ChatMessage> list) {
        // The rationale for RuntimeExceptions in this method:
        // 1. langchain4j error handling is a mess, and it uses RuntimeExceptions
        //    everywhere. Because this method implements a langchain4j interface,
        //    we follow the same "practice".
        // 2. There is no way to encode error information from type system: nor
        //    in the result type, nor "throws" in method signature. Actually,
        //    it's possible, but langchain4j doesn't do it.

        if (langchainChatModel.isEmpty()) {
            if (!aiPreferences.getEnableAi()) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, you need to enable chatting with attached PDF files in JabRef preferences (AI tab)."));
            } else if (aiPreferences.getApiKeyForAiProvider(aiPreferences.getAiProvider()).isEmpty() && aiPreferences.getAiProvider() != AiProvider.GPT4ALL) {
                throw new RuntimeException(Localization.lang("In order to use AI chat, set an API key inside JabRef preferences (AI tab)."));
            } else {
                rebuild();
                if (langchainChatModel.isEmpty()) {
                    throw new RuntimeException(Localization.lang("Unable to chat with AI."));
                }
            }
        }

        return langchainChatModel.get().chat(list);
    }

    @Override
    public void close() {
        httpClient.shutdownNow();
        executorService.shutdownNow();
    }
}
