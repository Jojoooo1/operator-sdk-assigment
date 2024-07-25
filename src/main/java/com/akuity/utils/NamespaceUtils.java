package com.akuity.utils;

import static com.akuity.customresources.NamespaceClass.CLASS_NAME_LABEL_KEY;
import static java.lang.String.format;

import com.akuity.customresources.NamespaceClass;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class NamespaceUtils {

  public static Optional<NamespaceClass> getNamespaceClassFromNamespaceOptional(
      final Context<Namespace> context, final Namespace primary) {

    return context.getSecondaryResources(NamespaceClass.class).stream()
        .filter(
            nsClass ->
                nsClass
                    .getMetadata()
                    .getName()
                    .equals(primary.getMetadata().getLabels().get(CLASS_NAME_LABEL_KEY)))
        .findFirst();
  }

  public static NamespaceClass getNamespaceClassFromNamespaceOrThrowException(
      final Context<Namespace> context, final Namespace primary) {
    return getNamespaceClassFromNamespaceOptional(context, primary)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    format(
                        "Something went wrong NamespaceClass '%s' not found in cache",
                        primary.getMetadata().getLabels().get(CLASS_NAME_LABEL_KEY))));
  }

  public static String getNsClassLabelFromNamespaceOrEmptyString(final Namespace primary) {
    return primary.getMetadata().getLabels().getOrDefault(CLASS_NAME_LABEL_KEY, StringUtils.EMPTY);
  }

  public static Set<ResourceID> buildNamespaceResourceIDsFromCache(
      final EventSourceContext<Namespace> context) {
    return context
        .getPrimaryCache()
        .list()
        .map(ResourceID::fromResource)
        .collect(Collectors.toSet());
  }
}
