/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.client.operation;

import static org.mule.runtime.extension.api.client.DefaultOperationParameters.builder;
import static org.mule.test.allure.AllureConstants.ExtensionsClientFeature.EXTENSIONS_CLIENT;
import static org.mule.test.heisenberg.extension.model.types.WeaponType.FIRE_WEAPON;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.extension.api.client.DefaultOperationParametersBuilder;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon.WeaponAttributes;
import org.mule.test.module.extension.client.operation.ExtensionsClientTestCase;

import java.util.Map;
import java.util.Optional;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(EXTENSIONS_CLIENT)
public abstract class DeprecatedExtensionsClientTestCase extends ExtensionsClientTestCase {

  @Override
  protected <T, A> Result<T, A> doExecute(String extension, String operation, Optional<String> configName,
                                          Map<String, Object> params, boolean isPagedOperation, boolean supportsStreaming)
      throws Throwable {
    DefaultOperationParametersBuilder builder = builder();
    configName.ifPresent(builder::configName);
    params.forEach(builder::addParameter);
    return doExecute(extension, operation, builder.build());
  }

  protected abstract <T, A> Result<T, A> doExecute(String extension, String operation, OperationParameters params)
      throws Throwable;

  @Test
  @Description("Executes an operation with a complex type parameter using the client and the DefaultOperationParametersBuilder")
  public void executeOperationWithComplexType() throws Throwable {
    WeaponAttributes attributes = new WeaponAttributes();
    attributes.setBrand("brand");
    OperationParameters params = builder()
        .configName(HEISENBERG_CONFIG)
        // Builds the complex type Ricin.
        .addParameter("weapon", Ricin.class, builder()
            .addParameter("destination", KnockeableDoor.class, builder()
                .addParameter("address", "ADdresss")
                .addParameter("victim", "victim!1231"))
            .addParameter("microgramsPerKilo", 123L))
        .addParameter("type", FIRE_WEAPON)
        .addParameter("attributesOfWeapon", attributes)
        .build();
    Result<String, Object> result = doExecute(HEISENBERG_EXT_NAME, "killWithWeapon", params);
    assertThat(result.getOutput(), is("Killed with: You have been killed with Ricin , Type FIRE_WEAPON and attribute brand"));
  }

}
