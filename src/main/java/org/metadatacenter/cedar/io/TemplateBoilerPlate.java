package org.metadatacenter.cedar.io;

import java.util.Map;

import static org.metadatacenter.cedar.io.BoilerPlate.fromJsonObject;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 2022-08-03
 */
public class TemplateBoilerPlate {

    public static final Map<String, Object> jsonld_context = fromJsonObject("""
                                                                              {
                                                                                "xsd": "http://www.w3.org/2001/XMLSchema#",
                                                                                "pav": "http://purl.org/pav/",
                                                                                "bibo": "http://purl.org/ontology/bibo/",
                                                                                "oslc": "http://open-services.net/ns/core#",
                                                                                "schema": "http://schema.org/",
                                                                                "schema:name": {
                                                                                  "@type": "xsd:string"
                                                                                },
                                                                                "schema:description": {
                                                                                  "@type": "xsd:string"
                                                                                },
                                                                                "pav:createdOn": {
                                                                                  "@type": "xsd:dateTime"
                                                                                },
                                                                                "pav:createdBy": {
                                                                                  "@type": "@id"
                                                                                },
                                                                                "pav:lastUpdatedOn": {
                                                                                  "@type": "xsd:dateTime"
                                                                                },
                                                                                "oslc:modifiedBy": {
                                                                                  "@type": "@id"
                                                                                }
                                                                              }
                                                                              """);

    public static final Map<String, Object> jsonschema_core_properties = fromJsonObject("""
{
"@id": {
      "type": [
        "string",
        "null"
      ],
      "format": "uri"
    },
    "@type": {
      "oneOf": [
        {
          "type": "string",
          "format": "uri"
        },
        {
          "type": "array",
          "minItems": 1,
          "items": {
            "type": "string",
            "format": "uri"
          },
          "uniqueItems": true
        }
      ]
    },
  "schema:isBasedOn": {
    "type": "string",
    "format": "uri"
  },
  "schema:name": {
    "type": "string",
    "minLength": 1
  },
  "schema:description": {
    "type": "string"
  },
  "pav:derivedFrom": {
    "type": "string",
    "format": "uri"
  },
  "pav:createdOn": {
    "type": [
      "string",
      "null"
    ],
    "format": "date-time"
  },
  "pav:createdBy": {
    "type": [
      "string",
      "null"
    ],
    "format": "uri"
  },
  "pav:lastUpdatedOn": {
    "type": [
      "string",
      "null"
    ],
    "format": "date-time"
  },
  "oslc:modifiedBy": {
    "type": [
      "string",
      "null"
    ],
    "format": "uri"
  }
}
       \s
"""
    );

    /**
     * The JSON Schema for the boilerplate template JSON-LD context
     */
    public static final Map<String, Object> jsonld_context_jsonschema_properties = fromJsonObject("""
                                                                                         {
    "rdfs": {
        "type": "string",
        "format": "uri",
        "enum": [
            "http://www.w3.org/2000/01/rdf-schema#"
        ]
    },
    "xsd": {
        "type": "string",
        "format": "uri",
        "enum": [
            "http://www.w3.org/2001/XMLSchema#"
        ]
    },
    "pav": {
        "type": "string",
        "format": "uri",
        "enum": [
            "http://purl.org/pav/"
        ]
    },
    "schema": {
        "type": "string",
        "format": "uri",
        "enum": [
            "http://schema.org/"
        ]
    },
    "oslc": {
        "type": "string",
        "format": "uri",
        "enum": [
            "http://open-services.net/ns/core#"
        ]
    },
    "skos": {
        "type": "string",
        "format": "uri",
        "enum": [
            "http://www.w3.org/2004/02/skos/core#"
        ]
    },
    "rdfs:label": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "xsd:string"
                ]
            }
        }
    },
    "schema:isBasedOn": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "@id"
                ]
            }
        }
    },
    "schema:name": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "xsd:string"
                ]
            }
        }
    },
    "schema:description": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "xsd:string"
                ]
            }
        }
    },
    "pav:derivedFrom": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "@id"
                ]
            }
        }
    },
    "pav:createdOn": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "xsd:dateTime"
                ]
            }
        }
    },
    "pav:createdBy": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "@id"
                ]
            }
        }
    },
    "pav:lastUpdatedOn": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "xsd:dateTime"
                ]
            }
        }
    },
    "oslc:modifiedBy": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "@id"
                ]
            }
        }
    },
    "skos:notation": {
        "type": "object",
        "properties": {
            "@type": {
                "type": "string",
                "enum": [
                    "xsd:string"
                ]
            }
        }
    }
}
                                                                                                                                                                                    """);

}
