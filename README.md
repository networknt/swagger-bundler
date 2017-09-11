# swagger-bundler
A utility that merges multiple swagger files into a single file with all external
references resolved to local reference.

## Why this bundler

As swagger specification becomes OpenAPI specification, a lot people have adopted it
to write their RESTful API spec. For small APIs, it is OK but for a large scale API
project, it hard to manage multiple API specs together. For an organization with too
many APIs, it is very natural to define sharable object definitions in separate files
in order to avoid duplications. Which multiple files inter-connected together to form
one API spec. The directory structure and external references are too complicated to
manage.

Luckily, there are a lot of tools like editor, parser and bundler in the market that
support multiple files. There tools can bundle multiple yaml files together to create
a final version of json and de-reference external dependencies.

As our light-rest-4j framework encourages design driven approach, the specification
should be done before coding started. Actually, if the spec is ready, you can use
[light-codegen](https://github.com/networknt/light-codegen) to generate the project.

The generator for [light-rest-4j](https://github.com/networknt/light-rest-4j) also
generate model (POJO) from the swagger.json with object defined in definitions. In
addition, light-rest-4j requires the final version of the swagger.json to be included
into the service code to do runtime schema validation as well as scope verification
at runtime.

This requires the final version of swagger.json must be self-contained and all model
should be defined in definitions section instead of de-reference inline.

The existing bundle like [swagger-cli](https://github.com/BigstickCarpet/swagger-cli)
cannot handle our files correctly and I couldn't find any existing tools to meet our
requirement.

## Feature

### Remote Reference

This bundler can resolve all remote references in swagger.yaml which is the main file
to be processed. If the reference is an object, it will resolve its internal references
first and then move it into definitions in the generated swagger.json. At the same time
the external reference in swagger.yaml will be changed to local reference with #/definitions/{key}

If the remote reference is not an object, it will be resolved inline in the generated
swagger.json file.

### Local Reference

If the reference is an object in definitions, it will resolve all the remote references
in definitions.

If the reference is not an object, an error will occur and the process will exit.

## Usage

The bundler assumes that the input file is swagger.yaml and all the remote reference files
are in the right path.

```
java -jar target/swagger-bundler.jar folder of swagger.yaml
```

Another way to run the bundler is from IDE. Just set the parameter and you can easily
debug into it.
