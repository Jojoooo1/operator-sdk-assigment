package com.akuity.dependents;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.akuity.KubernetesDependentResourceBaseTest;
import com.akuity.customresources.NamespaceClass;
import com.akuity.reconcilers.NamespaceReconciler;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import java.util.Map;
import org.junit.jupiter.api.extension.RegisterExtension;

class LimitRangeDRIT extends KubernetesDependentResourceBaseTest<LimitRange> {

  @RegisterExtension
  static LocallyRunOperatorExtension operator =
      LocallyRunOperatorExtension.builder()
          .waitForNamespaceDeletion(false)
          .oneNamespacePerClass(true)
          .withReconciler(new NamespaceReconciler())
          .withKubernetesClient(new KubernetesClientBuilder().build())
          .withAdditionalCustomResourceDefinition(NamespaceClass.class)
          .build();

  @Override
  public LocallyRunOperatorExtension operator() {
    return operator;
  }

  @Override
  public Class<LimitRange> getDependentResourceClass() {
    return LimitRange.class;
  }

  @Override
  public String getResourceName() {
    return ResourceUtils.name(operator.getNamespace(), LimitRangeDR.COMPONENT);
  }

  @Override
  public void dependentResourceAssertion(LimitRange resource, NamespaceClass nsClass) {
    assertThat(resource.getSpec().getLimits().get(0).getDefaultRequest().get("cpu").toString())
        .isEqualTo(nsClass.getSpec().getLimitRange().getDefaultRequestCpu());
  }

  @Override
  public LimitRange getDependentResourceToUpdate() {
    var resourceToUpdate = operator().get(getDependentResourceClass(), getResourceName());
    resourceToUpdate
        .getSpec()
        .getLimits()
        .get(0)
        .setDefaultRequest(Map.of("cpu", new Quantity("100m")));
    return resourceToUpdate;
  }
}
