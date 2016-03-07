from fabric.api import env, run, settings, sudo, cd, local
from fabric.context_managers import cd
from fabric.contrib import files
from fabfile_local import user, hosts
from datetime import datetime
from StringIO import StringIO


env.hosts = hosts
env.user = user
env.forward_agent = True

workflow_repo =  "git@bitbucket.org:constantineche/misabogados-workflow.git"

deploy_location = "/var/deploy/"

def deploy(branch="master"):
    local("ssh-agent")
    local("ssh-add")
    with cd(deploy_location):
        if not files.exists("misabogados-workflow"):
            run("git clone " + workflow_repo)
            run("git checkout " + branch)
            run("ln -s . " + deploy_location + "current")
            run("lein uberjar")
    with cd(deploy_location + "misabogados-workflow"):
        run("git checkout " + branch)
        run("git pull")
        run("lein uberjar")
        runapp()

def check():
    run("ps -ef | grep misabogados-workflow.jar")


def runapp():
    with cd(deploy_location + "misabogados-workflow"):
        if files.exists("pid"):
            pid = run("cat pid")
            with settings(warn_only=True):
                run("kill -9 " + pid.split(" ")[-1])
            run("rm pid")
        run("sh -c '((nohup %s > /dev/null 2> /dev/null) & echo $! > pid)'" % "java -jar target/misabogados-workflow.jar", pty=False)

def kill(pid):
    sudo("kill -9 " + pid)
