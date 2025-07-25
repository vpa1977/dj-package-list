## Introduction

Proxy is configured as repository (full mirror for maven or installed by debian helper). 

Proxy checks if artifact (group id and artifact id) are found in the local repo and serves it. 
- local repo:
  - /usr/share/maven-repo
- _second repo:
  - /var/lib/hp/maven-repo
Proxy otherwise it fetches it from a list of pre-configured upstreams and stores in _second repo.
Proxy stores all fetched artifact coordinates.


[ Nice to have ]
Proxy is fed a list of repository upstreams through debian helper call PUT http://localhost:8080/api/repository.
Proxy configures list of upstreams. 


## How it works. 

1. Proxy checks local cache for the exact path to the artifact. 
   If found -> return artifact. 
2. Apply substitution rules to search Debian repository (remap groupId, artifactId)
3. Proxy checks configured debian repository for groupId, artifactId
   If found -> puts artifact in local cache (remap version), return artifact from local cache
4. Proxy accesses upstream repositories
   If found -> puts artifact in local cache (as is), return artifact from local cache.