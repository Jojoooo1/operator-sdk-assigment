package com.akuity.customresources;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("akuity.io")
public class NamespaceClass extends CustomResource<NamespaceClassSpec, NamespaceClassStatus>
    implements Namespaced {

  public static final String CLASS_NAME_LABEL_KEY = "namespaceclass.akuity.io/name";

  @Override
  protected NamespaceClassStatus initStatus() {
    return new NamespaceClassStatus();
  }
}
