package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import solannee.sheridancollege.ca.jobtrackr.exception.ExternalServiceException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

@Component
public class GoogleGmailMailboxClient implements GmailMailboxClient {

    private static final String MESSAGES_ENDPOINT =
            "https://gmail.googleapis.com/gmail/v1/users/me/messages";
    private static final int MAX_CONTENT_LENGTH = 100_000;

    private final RestClient restClient;

    public GoogleGmailMailboxClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<GmailMessage> listMessages(String accessToken, String query, int maxResults) {
        try {
            MessageListPayload list = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("gmail.googleapis.com")
                            .path("/gmail/v1/users/me/messages")
                            .queryParam("q", query)
                            .queryParam("maxResults", maxResults)
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(MessageListPayload.class);

            if (list == null || list.messages() == null) {
                return List.of();
            }

            List<GmailMessage> messages = new ArrayList<>();
            for (MessageReference reference : list.messages()) {
                if (reference == null || reference.id() == null || reference.id().isBlank()) {
                    continue;
                }
                messages.add(getMessage(accessToken, reference.id()));
            }
            return List.copyOf(messages);
        } catch (RestClientException exception) {
            throw new ExternalServiceException("Unable to scan Gmail messages", exception);
        }
    }

    private GmailMessage getMessage(String accessToken, String messageId) {
        GmailMessagePayload message = restClient.get()
                .uri(MESSAGES_ENDPOINT + "/{id}?format=full", messageId)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(GmailMessagePayload.class);
        if (message == null || message.id() == null || message.payload() == null) {
            throw new ExternalServiceException("Google returned an incomplete Gmail message");
        }

        String plainText = firstContent(message.payload(), "text/plain");
        String html = plainText == null ? firstContent(message.payload(), "text/html") : null;
        String content = plainText == null ? htmlToText(html) : plainText;
        return new GmailMessage(
                message.id(),
                header(message.payload(), "Subject", "(No subject)"),
                header(message.payload(), "From", "(Unknown sender)"),
                parseReceivedAt(message.internalDate()),
                truncate(content == null ? "" : content, MAX_CONTENT_LENGTH)
        );
    }

    private String firstContent(GmailPayload payload, String mimeType) {
        if (payload == null) {
            return null;
        }
        if (mimeType.equalsIgnoreCase(payload.mimeType())
                && payload.body() != null
                && payload.body().data() != null) {
            return decode(payload.body().data());
        }
        for (GmailPayload part : safe(payload.parts())) {
            String content = firstContent(part, mimeType);
            if (content != null && !content.isBlank()) {
                return content;
            }
        }
        return null;
    }

    private String decode(String encoded) {
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String withoutUnsafeBlocks = html.replaceAll(
                "(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        String withLines = withoutUnsafeBlocks
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|tr|h[1-6])>", "\n");
        return HtmlUtils.htmlUnescape(withLines.replaceAll("(?s)<[^>]+>", " "))
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r ]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .trim();
    }

    private String header(GmailPayload payload, String name, String fallback) {
        return safe(payload.headers()).stream()
                .filter(header -> name.equalsIgnoreCase(header.name()))
                .map(GmailHeader::value)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private Instant parseReceivedAt(String internalDate) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(internalDate));
        } catch (RuntimeException exception) {
            return Instant.now();
        }
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private String truncate(String value, int maximum) {
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private record MessageListPayload(List<MessageReference> messages) {
    }

    private record MessageReference(String id) {
    }

    private record GmailMessagePayload(
            String id,
            String internalDate,
            GmailPayload payload
    ) {
    }

    private record GmailPayload(
            String mimeType,
            List<GmailHeader> headers,
            GmailBody body,
            List<GmailPayload> parts
    ) {
    }

    private record GmailHeader(String name, String value) {
    }

    private record GmailBody(String data) {
    }
}
