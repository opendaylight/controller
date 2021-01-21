#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# SPDX-License-Identifier: EPL-1.0
##############################################################################
# Copyright (c) 2018 The Linux Foundation and others.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
##############################################################################

import xml.etree.ElementTree as ET

from docs_conf.conf import *

data = ET.parse('pom.xml')
project_version = data.getroot().find('./{http://maven.apache.org/POM/4.0.0}version').text
version = project_version
release = project_version
