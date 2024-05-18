PUT /demo3-info7255/
{
  "mappings" : {
    "properties" : {
      "objectId" : {
        "type" : "keyword"
      },
      "plan_join" : {
        "type" : "join",
        "relations" : {
          "linkedPlanServices" : ["linkedService","planserviceCostShares"],
          "plan" : ["planCostShares", "linkedPlanServices"]
        }
        }
      }
  }
}

GET /demo3-info7255/_search
{
  "query": {
    "match_all": {}
  }
}

#has_child
GET /demo3-info7255/_search
{
  "query": {
    "has_child": {
      "type": "planserviceCostShares",
      "query": {
        "range": {
          "copay": {
            "gte": 1
          }
        }
      }
    }
  }
}

GET /demo3-info7255/_search
{
  "query": {
    "has_child": {
      "type": "planservice",
      "query": {
        "has_child": {
          "type": "service",
          "query": {
            "match": {
              "objectId": "1234520xvc30asdf-502"
            }
          }
        }
      }
    }
  }
}

GET /demo3-info7255/_search
{
  "query": {
    "has_child": {
      "type": "linkedPlanServices",
      "query": {
        "match_all": {}
      }
    }
  }
}

GET /demo3-info7255/_search
{
  "query": {
    "has_parent": {
      "parent_type": "planservice",
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "objectId": "27283xvx9asdff-504"
              }
            }
          ]
        }
      }
    }
  }
}

#has parent
GET /demo3-info7255/_search
{
  "query": {
    "has_parent": {
      "parent_type": "plan",
      "query": {
        "match_all": {}
      }
    }
  }
}


#has parent
GET /demo3-info7255/_search
{
  "query": {
    "has_parent": {
      "parent_type": "linkedPlanServices",
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "objectId": "27283xvx9asdff-501"
              }
            }
          ]
        }
      }
    }
  }
}

#has parent
GET /demo3-info7255/_search
{
  "query": {
    "has_parent": {
      "parent_type": "planCostShares",
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "objectId": "27283xvx9asdff-501"
              }
            }
          ]
        }
      }
    }
  }
}