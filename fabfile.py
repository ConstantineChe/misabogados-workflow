from fabric.api import env, run, settings, sudo, cd
from fabric.context_managers import cd
from fabfile_local import user, hosts
from datetime import date


env.hosts = hosts
env.user = user
env.forward_agent = True

deploy_location = "~/"

def deploy(branch="master"):
    dir = "release" + date.today().isoformat()
    with settings(warn_only=True):
        run("mkdir " + deploy_location + dir)
        with cd(deploy_location + dir):
            run("pwd")
            run("git clone git@bitbucket.org:constantineche/misabogados-workflow.git .")
            run("git checkout " + branch)
            run("ln -s . " + deploy_location + "current")
            run("lein uberjar")


def check():
    run("ls")
