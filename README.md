# swagger-bundler
A utility that merges multiple swagger files into a single file with all external
references resolved to local references.

## Why this bundler

As swagger specification becomes OpenAPI specification, a lot people have adopted it
to write their RESTful API spec. For small APIs, it is OK but for a large scale API
project, it hard to manage multiple API specs together. For an organization with too
many APIs, it is very natural to define sharable object definitions in separate files
in order to avoid duplications. With multiple files inter-connected together to form
one API specification, the directory structure and external references are too complicated 
to manage.

Luckily, there are a lot of tools like editor, parser and bundler in the market that
support multiple files. These tools can bundle multiple yaml files together to create
a final version of json and de-reference external dependencies.

As our light-rest-4j framework encourages design driven approach, the specification
should be done before coding started. Actually, if the spec is ready, you can use
[light-codegen](https://github.com/networknt/light-codegen) to generate the project.

The generator for [light-rest-4j](https://github.com/networknt/light-rest-4j) also
generate model (POJO) from the swagger.json with object defined in definitions. In
addition, light-rest-4j requires the final version of the swagger.json to be included
into the service code to do runtime schema validation as well as oauth 2.0 scope 
verification at runtime.

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

If you have separate reference files, you must place these files into the folder the
same as you have refer the file in your swagger.yaml. For example, if you have common
folder that contains all the common swagger files, you might need to copy the common
folder into your folder that contains swagger.yaml for your API.

### Local Reference

If the reference is an object in definitions, it will resolve all the remote references
in definitions.

If the reference is not an object, an error will occur and the process will exit.

## Usage

The bundler assumes that the input file is swagger.yaml and all the remote reference files
are in the right path.

### Use it as Java utility

```
java -jar target/swagger-bundler.jar <folder of swagger.yaml>

# to visualize debug message during the bundling process, use the utility with the debugOutput flag
java -DdebugOutput -jar target/swagger-bundler.jar  <folder of swagger.yaml>

```

### Use it in IDE

Another way to run the bundler is from an IDE. Just set the folder of the swagger.yaml file as a program argument and you can easily debug into it.

### Use Docker

There is a Docker [image](https://hub.docker.com/r/networknt/swagger-bundler/) that is 
published to Docker Hub.

Here is the command line to call it.

```
docker run -it -v ~/networknt/model-config/rest/petstore/2.0.0:/light-api/input networknt/swagger-bundler /light-api/input
```
Above command assume that your swagger.yaml is in ~/networknt/model-config/rest/petstore/2.0.0 
folder and the newly generated swagger.json will be put into the same folder. 

With above command line, you can easily build a script to call it as part of your DevOps
flow. 

