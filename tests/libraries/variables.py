#
# Copyright (c) 2026 PANTHEON.tech, s.r.o. and others.  All rights reserved.
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License v1.0 which accompanies this distribution,
# and is available at http://www.eclipse.org/legal/epl-v10.html
#
# These variables are considered global and immutable, so their names are in ALL_CAPS.
#

from pydantic import computed_field
from pydantic_settings import BaseSettings, PydanticBaseSettingsSource


class ControllerVariables(BaseSettings):
    """
    Base settings shared by every project's own Variables class. Subclasses
    are expected to declare their own RESTCONF_PORT/RESTCONF_ROOT, since those
    genuinely differ per project deployment.

    Values can be overridden by environment variables matching the field name.
    No .env files or secrets directories: not the file every project already
    has and reviews, and there is no reason to add another place a value
    could come from beyond that one explicit escape hatch.
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
