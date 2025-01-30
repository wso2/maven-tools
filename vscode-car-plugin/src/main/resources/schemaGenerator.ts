import ts from "typescript";

function parseTypeScriptInterface(filePath: string): ts.SourceFile {
  // Read the TypeScript file
  const fileContents = ts.sys.readFile(filePath);
  if (!fileContents) {
    throw new Error(`Failed to read file: ${filePath}`);
  }

  // Parse the file to get the Abstract Syntax Tree (AST)
  const sourceFile = ts.createSourceFile(
    filePath,
    fileContents,
    ts.ScriptTarget.Latest, // Parse using the latest TypeScript version
    true // Set parent nodes
  )

  return sourceFile;
}

function getSeparateInterfacesWithComments(sourceFile: ts.SourceFile): ts.SourceFile[] {
  const resultSourceFiles: ts.SourceFile[] = [];

  const visitNode = (node: ts.Node) => {
    if (ts.isInterfaceDeclaration(node)) {
      const commentsBeforeNode = ts.getLeadingCommentRanges(sourceFile.text, node.getFullStart());
      let commentText = '';
      if (commentsBeforeNode) {
        for (const range of commentsBeforeNode) {
          commentText += sourceFile.text.substring(range.pos, range.end) + '\n';
        }
      }
      const nodeText = node.getFullText(sourceFile);
      const newSourceFile = ts.createSourceFile(`${node.name.text}.ts`, nodeText, ts.ScriptTarget.Latest, true);
      resultSourceFiles.push(newSourceFile);
    }
    ts.forEachChild(node, visitNode);
  };

  visitNode(sourceFile);

  return resultSourceFiles;
}

function getFunctionFromSourceFile(sourceFile: ts.SourceFile): ts.FunctionDeclaration | undefined {
  let functionNode: ts.FunctionDeclaration | undefined;

  const visitNode = (node: ts.Node) => {
    if (ts.isFunctionDeclaration(node)) {
      if (node.name?.getText() === "mapFunction"){
        functionNode = node;
      }
    }
    ts.forEachChild(node, visitNode);
  };
  visitNode(sourceFile);
  return functionNode;
}

function checkIfInputIsArray(functionNode: ts.FunctionDeclaration): boolean {
  // Check if the input parameter is an array
  const inputParameter = functionNode.parameters[0];
  if (inputParameter.type && ts.isArrayTypeNode(inputParameter.type)) {
    return true;
  }
  return false;
}

function checkIfOutputIsArray(functionNode: ts.FunctionDeclaration): boolean {
  // Check if the return type is an array
  if (functionNode.type && ts.isArrayTypeNode(functionNode.type)) {
    return true;
  }
  return false;
}


function generateJsonSchemaFromAST(ast: ts.SourceFile): any {
  const schema: any = {
    "$schema" : "http://wso2.org/json-schema/wso2-data-mapper-v5.0.0/schema#",
    type: "object",
    title: getInterfaceName(ast) || "Unknown",
    properties: {}
  };

  const commentRange = ts.getTrailingCommentRanges(ast.getFullText().trim(), 0);
  if (commentRange) {
    const comment = ast.getFullText().substring(commentRange[0].pos, commentRange[0].end);
    const namespaces = processNamespaces(comment);
    if (namespaces) {
      schema.namespaces = namespaces;
    }
    const title = processTitle(comment);
    if (title) {
      schema.title = title;
    }
    const inputType = deriveMediaTypeFromComment(comment, "input");
    if (inputType) {
      schema.inputType = inputType;
    }
    const outputType = deriveMediaTypeFromComment(comment, "output");
    if (outputType) {
      schema.outputType = outputType;
    }
  }

  function getType(typeNode: ts.TypeNode): string {
    switch (typeNode.kind) {
      case ts.SyntaxKind.StringKeyword:
        return "string";
      case ts.SyntaxKind.NumberKeyword:
        return "number";
      case ts.SyntaxKind.BooleanKeyword:
        return "boolean";
      case ts.SyntaxKind.ArrayType:
        return "array";
      // Add more cases as needed
      default:
        return "object";
    }
  }

  function processNamespaces(comment: string) {
    // namespaces :[{"prefix": "axis2ns1","url": "urn:hl7-org:v2xml"}]
    const namespaceRegex = /namespaces\s*:\s*(\[.*\])/;
    const match = comment.match(namespaceRegex);
    if (match) {
      try {
        return JSON.parse(match[1]);
      } catch (error) {
        console.error("Failed to parse namespaces", error);
      }
    }
  }

  function processTitle(comment: string) {
    // title : "axis2ns1:ADT_A01",
    const titleRegex = /title\s*:\s*"(.*)"/;
    const match = comment.match(titleRegex);
    if (match) {
      return match[1];
    }
  }

  function getInterfaceName(ast: ts.SourceFile): string | null {
    for (const statement of ast.statements) {
      if (ts.isInterfaceDeclaration(statement)) {
        return statement.name.text;
      }
    }
    return null; // Return null if no interface declaration is found
  }

  function processMembers(members: ts.NodeArray<ts.TypeElement>, parentSchema: any) {
    members.forEach(member => {
      if (ts.isPropertySignature(member) && member.name && member.type) {
        const propertyName = formatPropertyName(member.name.getText(ast));
        if (ts.isTypeLiteralNode(member.type)) {
          const nestedSchema = { type: "object", properties: {} };
          processMembers(member.type.members, nestedSchema);
          parentSchema.properties[propertyName] = nestedSchema;
        } else if (ts.isArrayTypeNode(member.type)) {
          const elementType = getType(member.type.elementType);
          if (elementType === "object") {
            const nestedSchema = { type: "object", properties: {} };
            if (ts.isTypeLiteralNode(member.type.elementType)) {
              processMembers(member.type.elementType.members, nestedSchema);
            }
            parentSchema.properties[propertyName] = { type: "array", items: [nestedSchema] };
          } else {
            parentSchema.properties[propertyName] = { type: "array", items: [{ type: elementType }] };
          }
        } else {
          parentSchema.properties[propertyName] = { type: getType(member.type) };
        }
      }
    });
  }

  function formatPropertyName(name: string) {
    // avoid formatting xml element value
    if (name === "_ELEMVAL") {
      return name;
    }
    // Remove quotes and leading/trailing whitespace
    let formattedName = name.replace(/"/g, "").trim();
    if (formattedName.startsWith("attr_")) {
      // Remove the "attr_" prefix
      formattedName = formattedName.substring(5);
    }
    // Replace underscores with colons for XML namespaces
    if (formattedName.includes("_")) {
        const prefix = formattedName.split("_")[0];
        const namespaceExists = schema.namespaces && schema.namespaces.some(ns => ns.prefix === prefix);
        if ((schema.inputType && schema.inputType.toLowerCase() === "xml" && namespaceExists) ||
            (schema.outputType && schema.outputType.toLowerCase() === "xml" && namespaceExists)) {
            formattedName = formattedName.replace("_", ":");
        }
    }
    return formattedName;
  }

  function visit(node: ts.Node) {
    if (ts.isInterfaceDeclaration(node)) {
      // Assuming a single interface in the file for simplicity
      processMembers(node.members, schema);
    }
    ts.forEachChild(node, visit);
  }

  visit(ast);

  return schema;
}

function deriveMediaTypeFromComment(comment: string, ioType: string): string | undefined {
  const mediaTypeRegex = new RegExp(`${ioType}Type\\s*:\\s*"([^"]+)"`);
  const match = comment.match(mediaTypeRegex);
  if (match) {
    return match[1];
  }
  return undefined;
}

function convertSchemaObjectToArray(schema: any): any {
  // Convert the object schema to an array schema
  const arraySchema: any = {
    "$schema": schema["$schema"],
    type: "array",
    title: schema.title,
    items: [{
      type: "object",
      properties: schema.properties
    }]
  };
  return arraySchema;
}

function generateJsonSchema(filePath: string): void {
  const ast = parseTypeScriptInterface(filePath);
  const sourceFiles = getSeparateInterfacesWithComments(ast);
  const functionNode = getFunctionFromSourceFile(ast);
  if (!functionNode) {
    console.error("No function found in the source file");
    return;
  }
  const inputIsArray = checkIfInputIsArray(functionNode);
  const outputIsArray = checkIfOutputIsArray(functionNode);

  let inputSchema = "";
  let outputSchema = "";
  for (const sourceFile of sourceFiles) {
    let jsonSchema = generateJsonSchemaFromAST(sourceFile);
    if (sourceFile.getFullText().includes("inputType")) {
      if (inputIsArray) {
        jsonSchema = convertSchemaObjectToArray(jsonSchema);
      }
      inputSchema = jsonSchema;
    }
    if (sourceFile.getFullText().includes("outputType")) {
      if (outputIsArray) {
        jsonSchema = convertSchemaObjectToArray(jsonSchema);
      }
      outputSchema = jsonSchema;
    }
  }
  // create two schema files as siblings to the input file
  const inputSchemaPath = filePath.replace(".ts", "_inputSchema.json");
  const outputSchemaPath = filePath.replace(".ts", "_outputSchema.json");
  ts.sys.writeFile(inputSchemaPath, JSON.stringify(inputSchema, null, 2));
  ts.sys.writeFile(outputSchemaPath, JSON.stringify(outputSchema, null, 2));
}

const filePath = process.argv[2];
generateJsonSchema(filePath);
