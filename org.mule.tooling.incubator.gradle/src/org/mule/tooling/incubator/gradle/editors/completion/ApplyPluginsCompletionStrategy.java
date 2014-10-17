package org.mule.tooling.incubator.gradle.editors.completion;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.mule.tooling.incubator.gradle.parser.DSLMethodAndMap;
import org.mule.tooling.model.Activator;

import com.mulesoft.build.MulePlugin;


public class ApplyPluginsCompletionStrategy implements DSLCompletionStrategy {
    
    private static List<GroovyCompletionSuggestion> pluginNames = detectPossiblePlugins();
    
    private static List<GroovyCompletionSuggestion> args = 
            Arrays.asList(new GroovyCompletionSuggestion(GroovyCompletionSuggestionType.MAP_ARGUMENT, "plugin", "java.lang.String"));
    
    @Override
    public List<GroovyCompletionSuggestion> buildSuggestions(DSLMethodAndMap map, Class<?> contextClass, String expectedInputKey) {
        
        if (StringUtils.isEmpty(expectedInputKey)) {
            return args;
        }
        
        return pluginNames;
    }
    
    
    private static List<GroovyCompletionSuggestion> detectPossiblePlugins() {
        
        List<GroovyCompletionSuggestion> ret = new ArrayList<GroovyCompletionSuggestion>();
        
        try {
            //provide a basic list of plugins
            String[] plugins = {"mulestudio", "mmc", "cloudhub"};
            
            for(String name : plugins) {
                ret.add(new GroovyCompletionSuggestion(GroovyCompletionSuggestionType.STRING_VALUE, name, "Gradle plugin: " + name));
            }
            
        } catch (Exception ex) {
            Activator.logError("Could not list gradle plugins", ex);
        }
        
        return Collections.unmodifiableList(ret);
    }
    
}
