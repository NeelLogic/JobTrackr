package solannee.sheridancollege.ca.jobtrackr.integration.gmail;

import org.springframework.stereotype.Component;
import solannee.sheridancollege.ca.jobtrackr.model.ApplicationStatus;
import solannee.sheridancollege.ca.jobtrackr.model.DetectionConfidence;
import solannee.sheridancollege.ca.jobtrackr.model.EmploymentType;
import solannee.sheridancollege.ca.jobtrackr.model.GmailImportProvider;

import java.net.URI;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GmailApplicationEmailParser {

    private static final int COMPANY_MAX = 120;
    private static final int JOB_TITLE_MAX = 160;
    private static final int LOCATION_MAX = 160;
    private static final int URL_MAX = 1000;
    private static final Pattern EMAIL_ADDRESS = Pattern.compile("<([^>]+)>");
    private static final Pattern DISPLAY_NAME = Pattern.compile("^\\s*\"?([^\"<]+?)\"?\\s*<");
    private static final Pattern URL = Pattern.compile("https?://[^\\s<>\"']+");
    private static final Pattern LOCATION = Pattern.compile(
            "(?im)^\\s*(?:job\\s+)?location\\s*[:\\-]\\s*([^\\r\\n|]{2,160})");
    private static final List<NamePattern> TITLE_COMPANY_PATTERNS = List.of(
            new NamePattern(Pattern.compile(
                    "(?i)(?:successfully\\s+)?submitted an application for\\s+(.+?)\\s+at\\s+"
                            + "(.+?)(?:[.!|\\r\\n]|$)"), false),
            new NamePattern(Pattern.compile(
                    "(?i)(?:application (?:received|submitted)|thank you for applying)"
                            + "\\s+(?:for|to)\\s+(.+?)\\s+at\\s+(.+?)(?:[.!|\\r\\n]|$)"), false),
            new NamePattern(Pattern.compile(
                    "(?i)(?:your )?application\\s+(?:to|with)\\s+(.+?)\\s+for\\s+(.+?)"
                            + "(?:[.!|\\r\\n]|$)"), true),
            new NamePattern(Pattern.compile(
                    "(?i)(?:your\\s+)?application\\s+for\\s+(.+?)\\s+at\\s+(.+?)"
                            + "(?:[.!|\\r\\n]|$)"), false),
            new NamePattern(Pattern.compile(
                    "(?i)(?:interview|assessment)(?: invitation)?(?: for|:)\\s+(.+?)"
                            + "\\s+at\\s+(.+?)(?:[.!|\\r\\n]|$)"), false),
            new NamePattern(Pattern.compile(
                    "(?i)(?:interest in|applied (?:for|to))\\s+(?:the\\s+)?(.+?)"
                            + "\\s+(?:position\\s+)?at\\s+(.+?)(?:[.!|\\r\\n]|$)"), false),
            new NamePattern(Pattern.compile(
                    "(?i)application\\s+for\\s+(.+?)\\s+[\\-|–|—]\\s+(.+?)"
                            + "(?:[|\\r\\n]|$)"), false)
    );
    private static final Pattern APPLICATION_SIGNAL = Pattern.compile(
            "(?i)\\b(application|applied|applying|candidate|assessment|interview|offer|"
                    + "not moving forward|position has been filled|unfortunately)\\b");
    private static final Pattern STRONG_SIGNAL = Pattern.compile(
            "(?i)\\b(thank you for applying|application (?:was |has been )?(?:received|submitted)|"
                    + "interview|assessment|coding challenge|job offer|offer of employment|"
                    + "not moving forward|position has been filled)\\b");

    public Optional<ParsedGmailApplication> parse(GmailMessage message) {
        String subject = clean(message.subject());
        String content = clean(message.content());
        String combined = subject + "\n" + content;
        if (!APPLICATION_SIGNAL.matcher(combined).find()) {
            return Optional.empty();
        }

        GmailImportProvider provider = isWorkday(message, combined)
                ? GmailImportProvider.WORKDAY
                : GmailImportProvider.GENERIC;
        ExtractedNames names = extractNames(combined);
        String company = names == null ? companyFromSender(message.sender()) : names.company();
        String title = names == null ? titleFromSubject(subject) : names.title();
        boolean strongSignal = STRONG_SIGNAL.matcher(combined).find();
        if (!strongSignal && names == null) {
            return Optional.empty();
        }

        company = defaultIfBlank(company, "Review company");
        title = defaultIfBlank(title, "Review job title");
        DetectionConfidence confidence = confidence(provider, names, company, title);
        return Optional.of(new ParsedGmailApplication(
                provider,
                confidence,
                truncate(company, COMPANY_MAX),
                truncate(title, JOB_TITLE_MAX),
                extractLocation(combined),
                extractJobUrl(combined),
                message.receivedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                detectStatus(combined),
                detectEmploymentType(combined)
        ));
    }

    private boolean isWorkday(GmailMessage message, String combined) {
        String text = (message.sender() + "\n" + combined).toLowerCase(Locale.ROOT);
        return text.contains("myworkdayjobs.com")
                || text.contains("workday.com")
                || text.contains("workday@");
    }

    private ExtractedNames extractNames(String text) {
        for (NamePattern namePattern : TITLE_COMPANY_PATTERNS) {
            Matcher matcher = namePattern.pattern().matcher(text);
            if (!matcher.find()) {
                continue;
            }
            String first = normalizeName(matcher.group(1));
            String second = normalizeName(matcher.group(2));
            if (namePattern.companyFirst()) {
                return validNames(second, first);
            }
            return validNames(first, second);
        }
        return null;
    }

    private ExtractedNames validNames(String title, String company) {
        if (title == null || company == null) {
            return null;
        }
        return new ExtractedNames(title, company);
    }

    private String titleFromSubject(String subject) {
        String title = subject
                .replaceAll("(?i)^(re:\\s*|fw:\\s*)+", "")
                .replaceAll("(?i)\\b(application|applied|received|submitted|update|status|"
                        + "thank you|interview|assessment|offer|rejected)\\b", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s:\\-|–|—]+|[\\s:\\-|–|—]+$", "")
                .trim();
        return title.length() < 3 ? null : title;
    }

    private String companyFromSender(String sender) {
        if (sender == null || sender.isBlank()) {
            return null;
        }
        Matcher display = DISPLAY_NAME.matcher(sender);
        if (display.find()) {
            return normalizeCompany(display.group(1));
        }
        Matcher address = EMAIL_ADDRESS.matcher(sender);
        String email = address.find() ? address.group(1) : sender;
        int at = email.lastIndexOf('@');
        if (at < 0 || at == email.length() - 1) {
            return null;
        }
        String domain = email.substring(at + 1).toLowerCase(Locale.ROOT);
        String[] labels = domain.split("\\.");
        if (labels.length < 2 || isGenericEmailProvider(domain)) {
            return null;
        }
        return normalizeCompany(labels[Math.max(0, labels.length - 2)]);
    }

    private boolean isGenericEmailProvider(String domain) {
        return domain.endsWith("gmail.com")
                || domain.endsWith("outlook.com")
                || domain.endsWith("hotmail.com")
                || domain.endsWith("yahoo.com")
                || domain.endsWith("myworkday.com")
                || domain.endsWith("myworkdayjobs.com");
    }

    private String normalizeCompany(String value) {
        String normalized = normalizeName(value);
        if (normalized == null) {
            return null;
        }
        return normalized
                .replaceAll("(?i)\\b(careers?|recruiting|talent acquisition|jobs?|team|"
                        + "notifications?|workday)\\b", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String extractLocation(String text) {
        Matcher matcher = LOCATION.matcher(text);
        return matcher.find() ? truncate(normalizeName(matcher.group(1)), LOCATION_MAX) : null;
    }

    private String extractJobUrl(String text) {
        Matcher matcher = URL.matcher(text);
        String fallback = null;
        while (matcher.find()) {
            String candidate = trimUrlPunctuation(matcher.group());
            String host = host(candidate);
            if (host == null) {
                continue;
            }
            if (host.contains("myworkdayjobs.com") || host.contains("workday.com")) {
                return truncate(candidate, URL_MAX);
            }
            if (fallback == null && !isTrackingOrUnsubscribe(candidate)) {
                fallback = candidate;
            }
        }
        return truncate(fallback, URL_MAX);
    }

    private String host(String value) {
        try {
            return URI.create(value).getHost().toLowerCase(Locale.ROOT);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean isTrackingOrUnsubscribe(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.contains("unsubscribe")
                || normalized.contains("preferences")
                || normalized.contains("tracking");
    }

    private ApplicationStatus detectStatus(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "offer of employment", "job offer", "pleased to offer")) {
            return ApplicationStatus.OFFER;
        }
        if (containsAny(normalized, "not moving forward", "position has been filled",
                "other candidates", "regret to inform", "unfortunately")) {
            return ApplicationStatus.REJECTED;
        }
        if (containsAny(normalized, "interview", "speak with you", "schedule a call")) {
            return ApplicationStatus.INTERVIEW;
        }
        if (containsAny(normalized, "assessment", "coding challenge", "technical test")) {
            return ApplicationStatus.ASSESSMENT;
        }
        return ApplicationStatus.APPLIED;
    }

    private EmploymentType detectEmploymentType(String text) {
        String normalized = text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "co-op", "co op", "coop")) {
            return EmploymentType.CO_OP;
        }
        if (containsAny(normalized, "internship", "intern position", "summer intern")) {
            return EmploymentType.INTERNSHIP;
        }
        if (containsAny(normalized, "part-time", "part time")) {
            return EmploymentType.PART_TIME;
        }
        if (containsAny(normalized, "contract", "contractor")) {
            return EmploymentType.CONTRACT;
        }
        if (containsAny(normalized, "temporary", "temp position")) {
            return EmploymentType.TEMPORARY;
        }
        return EmploymentType.FULL_TIME;
    }

    private DetectionConfidence confidence(
            GmailImportProvider provider,
            ExtractedNames names,
            String company,
            String title
    ) {
        if (provider == GmailImportProvider.WORKDAY && names != null) {
            return DetectionConfidence.HIGH;
        }
        if (names != null
                && !company.startsWith("Review ")
                && !title.startsWith("Review ")) {
            return DetectionConfidence.HIGH;
        }
        if (!company.startsWith("Review ") && !title.startsWith("Review ")) {
            return DetectionConfidence.MEDIUM;
        }
        return DetectionConfidence.LOW;
    }

    private boolean containsAny(String value, String... signals) {
        for (String signal : signals) {
            if (value.contains(signal)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value
                .replaceAll("(?i)<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("^[\\s:\\-|–|—]+|[\\s:\\-|–|—]+$", "")
                .trim();
        return normalized.length() < 2 ? null : normalized;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimUrlPunctuation(String value) {
        return value.replaceAll("[),.;]+$", "");
    }

    private String truncate(String value, int maximum) {
        if (value == null) {
            return null;
        }
        return value.length() <= maximum ? value : value.substring(0, maximum);
    }

    private record ExtractedNames(String title, String company) {
    }

    private record NamePattern(Pattern pattern, boolean companyFirst) {
    }
}
