/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.hp.rev150218;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.didm.hp3800.Hp3800ModuleImpl;

/**
 * The HP 3800 config subsystem module manages:
 *   1) the HP 3800 device type info
 *   2) the HP 3800 OF driver
 */
public class HP3800Module extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.hp.rev150218.AbstractHP3800Module {
    private static final Logger LOG = LoggerFactory.getLogger(HP3800Module.class);

    public HP3800Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public HP3800Module(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.hp.rev150218.HP3800Module oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Creating Hp3800ModuleImpl");
        return new Hp3800ModuleImpl(getDataBrokerDependency(), getRpcRegistryDependency());
    }

}
