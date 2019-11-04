# microservice-manager
A tool to register microservice environments and deploy and manage kubernetes deployment

Register a github repository and register build in codeship.
Build details
Either use codeship or other build tool like jenkins.
What the build tool should do. Whatever code you want to deploy , a version needs to made.
version format should be vx.x.x.x eg v1.0.1 , v2.0.1.1 etc
Right now only asia.gcr is used. The build should make a docker image and push it to this registry.

After registering a project, We can now create multiple environments for the same. Each environment will have a seperate namespace in kubernetes
Add environment to create deployment in kubernetes.

Cluster manager to register the kubernetes cluster. Currently only gcp cluster is supported.


