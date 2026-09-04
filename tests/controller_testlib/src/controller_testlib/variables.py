#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# These variables are considered global and immutable, so their names are in ALL_CAPS.
#

import importlib
import logging

from pydantic import computed_field
from pydantic_settings import BaseSettings, PydanticBaseSettingsSource

log = logging.getLogger(__name__)


class ControllerVariables(BaseSettings):
    """
    Base settings shared by every project's own Variables class. Subclasses
    are expected to declare their own RESTCONF_PORT/RESTCONF_ROOT, since those
    genuinely differ per project deployment.

    Resolution order, highest priority first:
      1. An actual environment variable matching the field name - a
         deliberate, explicit escape hatch for one-off overrides (e.g. a CI
         job pointing at a differently configured environment) without
         editing any file.
      2. The current project's own tests/libraries/variables.py - its own
         override if it declares one for that field, otherwise whatever it
         inherits from the layer(s) above it (plain Python class inheritance,
         nothing pydantic-specific).
    .env files and secrets directories are deliberately left out: they are
    not the file every project already has and reviews, and there is no
    reason to add another place a value could come from beyond the one
    explicit escape hatch above.
    """

    @classmethod
    def settings_customise_sources(
        cls,
        settings_cls: type[BaseSettings],
        init_settings: PydanticBaseSettingsSource,
        env_settings: PydanticBaseSettingsSource,
        dotenv_settings: PydanticBaseSettingsSource,
        file_secret_settings: PydanticBaseSettingsSource,
    ) -> tuple[PydanticBaseSettingsSource, ...]:
        # init kwargs (rarely used here) win if given; then a real env var;
        # then whatever the field's own class-level default resolves to via
        # normal Python inheritance. No .env files, no secrets directory.
        return (init_settings, env_settings)

    ODL_IP: str = "127.0.0.1"
    ODL_USER: str = "admin"
    ODL_PASSWORD: str = "admin"
    TOOLS_IP: str = "127.0.1.0"
    KARAF_LOG_LEVEL: str = "INFO"

    @computed_field
    @property
    def REST_API(self) -> str:
        """Computes the RESTCONF data API root URI."""
        return f"{self.RESTCONF_ROOT}/data"


class _LazyVariables:
    """Reads the current project's libraries.variables.variables on first
    use, falling back to fallback_cls's own defaults if not found."""

    def __init__(self, fallback_cls):
        self._fallback_cls = fallback_cls
        self._resolved = None

    def _resolve(self):
        if self._resolved is None:
            try:
                self._resolved = importlib.import_module(
                    "libraries.variables"
                ).variables
            except ModuleNotFoundError:
                log.warning(
                    "libraries.variables not found, using %s defaults",
                    self._fallback_cls.__name__,
                )
                self._resolved = self._fallback_cls()
        return self._resolved

    def __getattr__(self, name):
        return getattr(self._resolve(), name)


variables = _LazyVariables(ControllerVariables)
