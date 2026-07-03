package io.ddys.halo.ddysopen.content;

import io.ddys.halo.ddysopen.config.DdysSettingsService;
import java.util.Properties;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component
public class DdysHeadProcessor implements TemplateHeadProcessor {

    private static final PropertyPlaceholderHelper PLACEHOLDER =
        new PropertyPlaceholderHelper("${", "}");

    private final PluginContext pluginContext;
    private final DdysSettingsService settingsService;

    public DdysHeadProcessor(PluginContext pluginContext, DdysSettingsService settingsService) {
        this.pluginContext = pluginContext;
        this.settingsService = settingsService;
    }

    @Override
    public Mono<Void> process(
        ITemplateContext context,
        IModel model,
        IElementModelStructureHandler structureHandler
    ) {
        return settingsService.fetch()
            .doOnNext(settings -> {
                if (!settings.themeCssEnabled()) {
                    return;
                }
                IModelFactory factory = context.getModelFactory();
                Properties values = new Properties();
                values.setProperty("pluginName", pluginContext.getName());
                values.setProperty("version", pluginContext.getVersion());
                String tags = """
                    <!-- DDYS start -->
                    <link rel="stylesheet" href="/plugins/${pluginName}/assets/static/frontend.css?version=${version}">
                    <script defer src="/plugins/${pluginName}/assets/static/frontend.js?version=${version}"></script>
                    <!-- DDYS end -->
                    """;
                model.add(factory.createText(PLACEHOLDER.replacePlaceholders(tags, values)));
            })
            .then();
    }
}
