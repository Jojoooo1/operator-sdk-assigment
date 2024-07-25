package com.akuity;

import static com.akuity.customresources.NamespaceClass.CLASS_NAME_LABEL_KEY;
import static com.akuity.testutils.TestBuilder.buildNsClass;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

import com.akuity.customresources.NamespaceClass;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import java.time.Duration;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

public abstract class KubernetesDependentResourceBaseTest<T extends HasMetadata> {

  public abstract LocallyRunOperatorExtension operator();

  public abstract Class<T> getDependentResourceClass();

  public abstract String getResourceName();

  public abstract void dependentResourceAssertion(T resource, NamespaceClass nsClass);

  public abstract T getDependentResourceToUpdate();

  @Test
  void verifyReconciliation() {
    var namespaceName = operator().getNamespace();
    var namespace = operator().get(Namespace.class, namespaceName);
    var nsClass = operator().create(buildNsClass(random(), operator().getNamespace()));

    // 1. add label to namespace
    namespace.getMetadata().getLabels().put(CLASS_NAME_LABEL_KEY, nsClass.getMetadata().getName());
    operator().replace(namespace);

    // 2. verify resource is deployed with CRD value
    var resourceName = getResourceName();
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var resource = operator().get(getDependentResourceClass(), resourceName);
              assertThat(resource).isNotNull();
              dependentResourceAssertion(resource, nsClass);
            });

    // 3. Verify resource is deployed again after modification
    var resourceToUpdate = getDependentResourceToUpdate();
    operator().replace(resourceToUpdate);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var resource = operator().get(getDependentResourceClass(), resourceName);
              assertThat(resource).isNotNull();
              dependentResourceAssertion(resource, nsClass);
            });

    // 4. verify resource deployed after deletion
    var resourceToDelete = operator().get(getDependentResourceClass(), resourceName);
    operator().delete(resourceToDelete);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var resource = operator().get(getDependentResourceClass(), resourceName);
              assertThat(resource).isNotNull();
            });
  }

  public static String random(final Integer... args) {
    return RandomStringUtils.randomAlphabetic(args.length == 0 ? 10 : args[0]).toLowerCase();
  }
}
