package com.akuity.utils;

import static com.akuity.customresources.NamespaceClass.CLASS_NAME_LABEL_KEY;
import static com.akuity.utils.Constants.K8S_COMPONENT;
import static com.akuity.utils.Constants.K8S_MANAGED_BY;
import static com.akuity.utils.Constants.K8S_NAME;
import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;
import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import java.io.IOException;
import java.io.InputStream;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class ResourceUtils {

  private static final ObjectMapper om = new ObjectMapper(new YAMLFactory());

  public static <T extends HasMetadata> ObjectMetaBuilder fromResource(
      final T primary, final String component) {
    return new ObjectMetaBuilder()
        .withName(name(primary, component))
        .withNamespace(namespace(primary))
        .addToLabels(K8S_NAME, name(primary, component))
        .addToLabels(K8S_COMPONENT, component)
        .addToLabels(
            K8S_NAMESPACE_CLASS,
            primary
                .getMetadata()
                .getLabels()
                .getOrDefault(CLASS_NAME_LABEL_KEY, Constants.OPERATOR_NAME))
        .addToLabels(K8S_MANAGED_BY, Constants.OPERATOR_NAME);
  }

  // Note: always use deterministic name, or it will mess with your cache.
  public <T extends HasMetadata> String name(final T resource, final String component) {
    return name(resource.getMetadata().getName(), component);
  }

  public String name(final String resourceName, final String component) {
    return component + "-" + resourceName;
  }

  public <T extends HasMetadata> String namespace(final T resource) {
    return resource instanceof Namespace
        ? resource.getMetadata().getName()
        : resource.getMetadata().getNamespace();
  }

  public static <T> T loadTemplateFromYAML(Class<T> clazz, String resource) {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) {
      cl = com.akuity.utils.ResourceUtils.class.getClassLoader();
    }

    try (InputStream is = cl.getResourceAsStream(resource)) {
      return loadTemplateFromYAML(clazz, is);
    } catch (IOException ex) {
      String errorMessage =
          format("Unable to load classpath resource '%s': %s", resource, ex.getMessage());
      log.error(errorMessage, ex);
      throw new IllegalArgumentException(errorMessage);
    }
  }

  public static <T> T loadTemplateFromYAML(Class<T> clazz, InputStream is) throws IOException {
    return om.readValue(is, clazz);
  }
}
