Test code generator, namely generating abstract classes to target/generated-sources/config folder.
Currently files generated to src are modified - getInstance() method must be implemented, so the body is replaced
manually with following snippet:
		return new AutoCloseable() {
			@Override
			public void close() throws Exception {
			}
		};
TODO: clean src/main/java directory and replace generated body during build.
