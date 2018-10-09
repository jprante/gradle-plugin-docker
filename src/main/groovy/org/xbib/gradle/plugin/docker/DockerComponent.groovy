package org.xbib.gradle.plugin.docker

import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Usage
import org.gradle.api.capabilities.Capability
import org.gradle.api.internal.attributes.ImmutableAttributes
import org.gradle.api.internal.attributes.ImmutableAttributesFactory
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.model.ObjectFactory

class DockerComponent implements SoftwareComponentInternal {

    private final UsageContext runtimeUsage

    private final Set<PublishArtifact> publishArtifacts

    private final DependencySet runtimeDependencies

    DockerComponent(PublishArtifact dockerArtifact,
                    DependencySet runtimeDependencies,
                    ObjectFactory objectFactory,
                    ImmutableAttributesFactory attributesFactory) {
        publishArtifacts = new LinkedHashSet<>()
        publishArtifacts.add(dockerArtifact)
        this.runtimeDependencies = runtimeDependencies
        Usage usage = objectFactory.named(Usage.class, Usage.JAVA_RUNTIME)
        this.runtimeUsage = new RuntimeUsageContext(usage,
                attributesFactory.of(Usage.USAGE_ATTRIBUTE, usage))
    }

    @Override
    String getName() {
        "docker"
    }

    @Override
    Set<UsageContext> getUsages() {
        Collections.singleton(runtimeUsage)
    }

    private class RuntimeUsageContext implements UsageContext {

        private final Usage usage
        private final ImmutableAttributes attributes

        private RuntimeUsageContext(Usage usage, ImmutableAttributes attributes) {
            this.usage = usage
            this.attributes = attributes
        }

        @Override
        Usage getUsage() {
            usage
        }

        @Override
        Set<? extends PublishArtifact> getArtifacts() {
            publishArtifacts
        }

        @Override
        Set<? extends ModuleDependency> getDependencies() {
            runtimeDependencies.withType(ModuleDependency.class)
        }

        @Override
        Set<? extends DependencyConstraint> getDependencyConstraints() {
            Collections.emptySet()
        }

        @Override
        Set<? extends Capability> getCapabilities() {
            Collections.emptySet()
        }

        @Override
        Set<ExcludeRule> getGlobalExcludes() {
            Collections.emptySet()
        }

        @Override
        String getName() {
            "runtime"
        }

        @Override
        AttributeContainer getAttributes() {
            attributes
        }
    }
}
