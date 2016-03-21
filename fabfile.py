from fabric.api import env, run, settings, sudo, cd, local
from fabric.context_managers import cd
from fabric.contrib import files
from fabfile_local import user, hosts
from datetime import datetime
from StringIO import StringIO


env.hosts = hosts
env.user = user
env.forward_agent = True

project = "misabogados_workflow"
repo =  "git@bitbucket.org:constantineche/misabogados-workflow.git"
deploy_location = "/var/deploy/"

class Instance:

    def __init__(self):
        self.echo = "default"
        self.opts = {'DATABASE_URL': 'mongodb://127.0.0.1/%s_dev' % project,
                     'PORT': '3000',
                     'LOG_PATH': '/var/deploy/log/%s.log' % project}

    def production(self):
        self.echo = "production"
        self.opts['DATABASE_URL'] = 'mongodb://127.0.0.1/%s_production' % project
        self.opts['PORT'] = '8080'
        self.opts['LOG_PATH'] = '/var/deploy/log/%s_production.log' % project

    def staging(self):
        self.echo = "staging"
        self.opts['DATABASE_URL'] = 'mongodb://127.0.0.1/%s_staging' % project
        self.opts['PORT'] = '3000'
        self.opts['LOG_PATH'] = '/var/deploy/log/%s_staging.log' % project
        self.opts['TEST'] = 'test'

    def get_opts_string(self):
        return ' '.join(map(lambda (k, v): k+'='+v, self.opts.iteritems()))


instance = Instance()


#============ Tasks =============

def production():
    instance.production()

def staging():
    instance.staging()


def deploy(branch="master"):
    local("ssh-agent")
    local("ssh-add")
    with cd(deploy_location):
        if not files.exists(project):
            run("git clone " + repo)
            with cd(deploy_location + project):
                run("git checkout " + branch)
                run("ln -s . " + deploy_location + "current")
                run("lein uberjar")
    with cd(deploy_location + project):
        run("git checkout " + branch)
        run("git pull")
        run("lein clean")
        run("lein uberjar")
        runapp()

def check():
    run("ps -ef | grep %s.jar" % project)


def runapp():
    with cd(deploy_location + project):
        if files.exists("pid"):
            pid = run("cat pid")
            with settings(warn_only=True):
                run("kill -9 " + pid.split(" ")[-1])
            run("rm pid")
        # Very powerful black magic
        run("sh -c '((%s nohup %s > /dev/null 2> /dev/null) & echo $! > pid)'" % (instance.get_opts_string(), "java -jar target/uberjar/%s.jar" % project), pty=False)

def kill(pid):
    sudo("kill -9 " + pid)

def log(file=project+".log"):
    run("ls /var/deploy/log")
    run("tail -n 100 /var/deploy/log/" + file)

def test():
    run("echo " + instance.echo)
    run(instance.get_opts_string() + " sh -c 'echo $LOG_PATH $TEST'")
