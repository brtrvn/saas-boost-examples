#!/usr/bin/env bash

read -p "Please enter your AWS SaaS Boost Environment label: " SAAS_BOOST_ENV
if [ -z "$SAAS_BOOST_ENV" ]; then
	echo "You must enter a AWS SaaS Boost Environment label to continue. Exiting."
	exit 1
fi

read -p "Please enter the application service name to build: " APP_NAME
if [ -z "$APP_NAME" ]; then
	echo "You must enter a configured application service name to continue. Exiting."
	exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
	echo "You must install jq to use this build script. Exiting."
	exit 1
fi

AWS_REGION=$(aws configure list | grep region | awk '{print $2}')
echo $AWS_REGION
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --output text --query ["Account"])
SERVICE_JSON=$(aws ssm get-parameter --name /saas-boost/$SAAS_BOOST_ENV/app/$APP_NAME/SERVICE_JSON --output text --query ["Parameter.Value"]) 
ECR_REPO=$(echo $SERVICE_JSON | jq -r '.containerRepo')
TAG=$(echo $SERVICE_JSON | jq -r '.containerTag')
if [ -z "$ECR_REPO" ]; then
    echo "Can't get ECR repo from Parameter Store. Exiting."
    exit 1
fi
DOCKER_REPO="$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$ECR_REPO"
DOCKER_TAG="$DOCKER_REPO:$TAG"

AWS_CLI_VERSION=$(aws --version 2>&1 | awk -F / '{print $2}' | cut -c 1)
if [ $AWS_CLI_VERSION = '1' ]; then
	echo "Running AWS CLI version 1"
	aws ecr get-login --no-include-email --region $AWS_REGION | awk '{print $6}' | docker login -u AWS --password-stdin $DOCKER_REPO
elif [ $AWS_CLI_VERSION = '2' ]; then
	echo "Running AWS CLI version 2"
	aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $DOCKER_REPO
else
	echo "Running unknown AWS CLI version"
fi

echo $DOCKER_TAG
IMAGE_NAME=$(echo $APP_NAME | sed 's/[^0-9A-Za-z-]//g' | tr [:upper:] [:lower:])
echo $IMAGE_NAME
echo
mvn clean package
docker image build --no-cache -t $IMAGE_NAME -f Dockerfile .
docker tag ${IMAGE_NAME}:latest $DOCKER_TAG
docker push $DOCKER_TAG
