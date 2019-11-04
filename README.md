# microservice-manager
A tool to register microservice(github projects), create different environments like staging , testing , production etc and deploy and manage these in kubernetes.

Register a github repository and register build in codeship. You have an option to choose codeship. If use codeship add
Docker containing Dockerfile and a yaml folder containing items below
1.Config folder containing all configmaps
2.microservice containing deployment yaml
3.ingress folder containing ingress.yaml
4.service folder containing service to deploy
5.environment-variables-list-for-user.txt
6.environment-variables.yaml
7.hpa.yaml
8.pdb.yaml

Build details
Either use codeship or other build tool like jenkins.
The build tool should make an docker image of whatever you want to deploy image should have a tag.format should be vx.x.x.x eg v1.0.1 , v2.0.1.1 etc
Right now only asia.gcr is used as registry . The build should push the docker image to this registry.

After registering a project, We can now create multiple environments for the same. Each environment will have a seperate namespace in kubernetes
Add environment to create deployment in kubernetes.

Cluster manager to register the kubernetes cluster. Currently only gcp cluster is supported.


