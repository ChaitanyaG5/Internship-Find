package com.example.internshiptracker.ai;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import com.example.internshiptracker.search.JobSearchFilters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LanguageModelRouter {

    private final List<LanguageModelService> providers;
    private final String defaultProvider;

    public LanguageModelRouter(
            List<LanguageModelService> providers,
            @Value("${app.ai.default-provider:OPENROUTER}") String defaultProvider) {
        this.providers = providers;
        this.defaultProvider = defaultProvider.toUpperCase(Locale.ROOT);
    }

    public JobSearchFilters extract(String query, String requestedProvider) {
        String providerName = requestedProvider == null || requestedProvider.isBlank()
                ? "AUTO"
                : requestedProvider.toUpperCase(Locale.ROOT);
        if (!List.of("AUTO", "OPENROUTER").contains(providerName)) {
            throw new LanguageModelException("Choose OPENROUTER or AUTO as the language model provider.");
        }

        return invoke(findProvider(providerName.equals("AUTO") ? defaultProvider : providerName), query);
    }

    private JobSearchFilters invoke(LanguageModelService provider, String query) {
        if (!provider.isConfigured()) {
            throw new LanguageModelException(provider.provider()
                    + " is not configured. Set its API key environment variable or choose another provider.");
        }
        JobSearchFilters filters = provider.extractFilters(
                "Current UTC date: " + LocalDate.now(ZoneOffset.UTC) + "\nUser search query: " + query);
        return filters.withDefaults();
    }

    private LanguageModelService findProvider(String name) {
        return providers.stream()
                .filter(provider -> provider.provider().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new LanguageModelException("Language model provider " + name + " is unavailable."));
    }
}
