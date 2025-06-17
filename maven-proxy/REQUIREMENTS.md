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
