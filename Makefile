# SBT_OPTS: JVM options for SBT
SBT_OPTS=-Xmx2G -Xms512M

# Variables for SHA256 checksum generation
SHA256_DIR := target/scala-2.13
SHA256_FILE := $(SHA256_DIR)/hashes.sha256
JAR_PATTERN := fx-*.jar # Pattern for JAR files to include in checksum

# Phony targets (targets that are not actual files)
.PHONY: assembly clean run jar sha256

# Default target (if any, e.g., all) could be defined here
# all: jar

# Assembles the project using sbt assembly
assembly:
	@echo "Assembling project with custom JVM options (SBT_OPTS='$(SBT_OPTS)')..."
	SBT_OPTS="$(SBT_OPTS)" sbt assembly

# Alias for assembly, creates the JAR file and then generates SHA256 checksums.
jar: assembly
	@echo "JAR assembly finished. Proceeding to generate SHA256 checksums..."
	$(MAKE) sha256 # Automatically call sha256 task after assembly

# Cleans the project build artifacts
clean:
	sbt clean

# Runs the project using sbt run
run:
	SBT_OPTS="$(SBT_OPTS)" sbt run

# Generates SHA256 checksums for JAR files matching $(JAR_PATTERN)
# in $(SHA256_DIR).
# The checksums are stored in $(SHA256_FILE).
# This task depends on the 'assembly' task to ensure JARs are built first.
# If 'make jar' is called, this task will be invoked automatically by the 'jar' target.
# If 'make sha256' is called directly, it will ensure 'assembly' runs first.
sha256: assembly # Changed dependency from 'jar' to 'assembly' to prevent loop
	@echo ""
	@echo "--- Generating SHA256 checksums ---"
	@echo "Target directory   : $(SHA256_DIR)"
	@echo "JAR file pattern   : $(JAR_PATTERN)"
	@echo "Checksum output file: $(SHA256_FILE)"
	@echo "-------------------------------------"
	@if [ ! -d "$(SHA256_DIR)" ]; then \
		echo "Error: Directory '$(SHA256_DIR)' not found." >&2; \
		echo "Please run 'make assembly' or 'make jar' first to build the project and create the directory." >&2; \
		exit 1; \
	fi
	@( \
		cd $(SHA256_DIR) && \
		echo "Searching for files matching '$(JAR_PATTERN)' in $$(pwd)..." && \
		rm -f hashes.sha256 && \
		_jar_files_found=0 && \
		_error_occurred=0 && \
		for _file in $(JAR_PATTERN); do \
			if [ -f "$$_file" ]; then \
				echo "Processing file: $$_file"; \
				if sha256sum -b "$$_file" >> hashes.sha256; then \
					_jar_files_found=1; \
				else \
					echo "Error: Failed to generate checksum for '$$_file'." >&2; \
					_error_occurred=1; \
				fi; \
			fi; \
		done; \
		if [ "$$_error_occurred" -ne 0 ]; then \
			echo "Error: One or more checksums could not be generated." >&2; \
			echo "Partially generated 'hashes.sha256' may exist but is incomplete or erroneous." >&2; \
			exit 1; \
		elif [ "$$_jar_files_found" -eq 1 ]; then \
			echo "SHA256 checksums successfully generated in '$$(pwd)/hashes.sha256'"; \
			echo "Contents of 'hashes.sha256':"; \
			cat hashes.sha256; \
		else \
			echo "No files matching '$(JAR_PATTERN)' found in $$(pwd)."; \
			echo "'hashes.sha256' was removed or is empty if no matching files were found."; \
		fi \
	)
	@echo ""
	@echo "--- Moving JARs and hashes.sha256 to project root ---"
	@mv $(SHA256_DIR)/$(JAR_PATTERN) . 2>/dev/null || echo "No JAR files found to move."
	@mv $(SHA256_FILE) . 2>/dev/null || echo "No hashes.sha256 file found to move."
	@echo "Moved files to: $$(pwd)"

	# setup.bat の SHA256 を hashes.sha256 に追記する処理
	@echo ""
	@echo "--- Appending SHA256 for setup.bat ---"
	@if [ -f setup.bat ]; then \
		if sha256sum -b setup.bat >> hashes.sha256; then \
			echo "Appended SHA256 checksum for setup.bat to hashes.sha256"; \
		else \
			echo "Error: Failed to generate SHA256 checksum for setup.bat" >&2; \
		fi \
	else \
		echo "Warning: setup.bat not found in project root." >&2; \
	fi
	@echo "--- SHA256 checksum generation finished ---"
	@echo "--- All checksum operations complete ---"

