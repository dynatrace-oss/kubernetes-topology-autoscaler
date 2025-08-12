.PHONY: markdownlint markdownlint-fix

MD_LINT_CLI_IMAGE := "ghcr.io/igorshubovych/markdownlint-cli:v0.31.1"

markdownlint-readmes:
	docker run -v $(CURDIR):/workdir --rm  $(MD_LINT_CLI_IMAGE) --ignore "website/docs/" "**/*.md" 
	
markdownlint-readmes-fix:
	docker run -v $(CURDIR):/workdir --rm  $(MD_LINT_CLI_IMAGE) --fix --ignore "website/docs/" "**/*.md"

markdownlint-docs:
	docker run -v $(CURDIR):/workdir --rm  $(MD_LINT_CLI_IMAGE) --config .markdownlint-docs.yml "website/docs/**/*.md"

markdownlint-docs-fix:
	docker run -v $(CURDIR):/workdir --rm  $(MD_LINT_CLI_IMAGE) --fix --config .markdownlint-docs.yml "website/docs/**/*.md"
