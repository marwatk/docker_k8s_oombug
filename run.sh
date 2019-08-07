#!/bin/bash

set -e

DOCKER_IMAGE=oombug
DOCKER_CONTAINER="${DOCKER_IMAGE}"
K8S_DEPLOYMENT=oombug
DOCKER_PORT=38985
K8S_PORT=18341

CONTAINER_PORT=8080
START_MESSAGE="Started WebRestoreApplication in"
TRIGGER_COMMAND="dd if=/dev/urandom bs=1M count=400 | curl -v http://127.0.0.1:8080/foo --upload-file -"

function cleanup {
    echo "Cleaning up"
    docker stop oombug 2>/dev/null || true
    kubectl delete "deployment/${K8S_DEPLOYMENT}" 2>/dev/null || true
    kubectl delete "pvc/${K8S_DEPLOYMENT}-pvc" 2>/dev/null || true
    kubectl delete "pv/${K8S_DEPLOYMENT}-pv" 2>/dev/null || true
}

function build {
    docker build . --tag "${DOCKER_IMAGE}"
}

function test_docker {
    echo "Testing via docker"

    docker run \
        --rm \
        --detach \
        --name "${DOCKER_CONTAINER}" \
        --publish "${DOCKER_PORT}:${CONTAINER_PORT}" \
        "${DOCKER_IMAGE}"

    docker_running=0
    for i in {1..5}; do
        if docker logs "${DOCKER_CONTAINER}" | grep "${START_MESSAGE}"; then
            docker_running=1
            break
        fi
        sleep 1
    done

    if [[ $docker_running == "0" ]]; then
        echo "======== Docker container never started! ==========="
    else
        if docker exec oombug bash -c "${TRIGGER_COMMAND}"; then
            echo "======== Docker succeeded =========="
        else
            echo "======== Docker failed =========="
        fi
    fi
}

function get_pod_name {
    kubectl get pod "--selector=app=${K8S_DEPLOYMENT}" -o jsonpath="{.items[0].metadata.name}" 2>/dev/null
}

function test_k8s {

    echo "Testing via k8s"

    pod_running=0
    kubectl apply -f deployment.yml
    for i in {1..10}; do
        if kubectl logs $(get_pod_name) | grep "${START_MESSAGE}"; then
            pod_running=1
            break
        fi
        sleep 1
    done

    if [[ $pod_running == "0" ]]; then
        echo "========== Pod never started! ==============="
    else
        if kubectl exec $(get_pod_name) -- bash -c "${TRIGGER_COMMAND}"; then
            echo "======== K8S Succeeded ==========="
        else
            echo "======== K8S Failed ============="
        fi
    fi

}

trap cleanup EXIT

build
test_docker
test_k8s