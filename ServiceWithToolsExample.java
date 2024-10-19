
//DEPS dev.langchain4j:langchain4j:0.35.0
//DEPS dev.langchain4j:langchain4j-open-ai:0.35.0
//DEPS ch.qos.logback:logback-classic:1.5.8
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;

import static dev.langchain4j.data.message.UserMessage.userMessage;
import static dev.langchain4j.data.message.SystemMessage.systemMessage;

import java.util.List;

public class ServiceWithToolsExample {

    static class Calculator {

        @Tool("Calculates the square root of a number")
        double sqrt(int x) {
            System.out.println("Called sqrt with x=" + x);
            return Math.sqrt(x);
        }
    }

    interface Assistant {
        String chat(String userMessage);
    }

    public static void main(String[] args) {

        ChatMemory chatMemory = TokenWindowChatMemory.withMaxTokens(30000, new OpenAiTokenizer());

        ChatLanguageModel model = OpenAiChatModel.builder()
                .baseUrl(System.getenv("OPENAI_BASE_URL"))
                .apiKey("no-key")
                .strictTools(true) // https://docs.langchain4j.dev/integrations/language-models/open-ai#structured-outputs-for-tools
                .logRequests(true)
                .logResponses(true)
                .build();

        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(Calculator.class);

        String question = "What is the square root of 101?";
        chatMemory.add(systemMessage("You are an helpful assistant"));
        chatMemory.add(userMessage(question));

        AiMessage answer = model.generate(chatMemory.messages(), toolSpecifications.get(0)).content();

        if (answer.hasToolExecutionRequests()) {
            chatMemory.add(answer);
            for (ToolExecutionRequest toolExecutionRequest : answer.toolExecutionRequests()) {
                ToolExecutionResultMessage toolExecutionResultMessage = ToolExecutionResultMessage
                        .from(toolExecutionRequest, "10.1"); // TODO really call the tool
                chatMemory.add(toolExecutionResultMessage);
            }
            answer = model.generate(chatMemory.messages()).content();
        }
        System.out.println(answer);
    }
}
