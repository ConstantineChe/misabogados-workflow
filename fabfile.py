from fabric.api import env, run, settings, sudo, cd, local
from fabric.context_managers import cd
from fabric.contrib import files
from datetime import datetime
from StringIO import StringIO


env.hosts = "23.253.248.253:22"
env.user = "deploy"
env.forward_agent = True

project = "misabogados_workflow"
repo =  "git@bitbucket.org:constantineche/misabogados-workflow.git"
deploy_location = "/var/deploy/"

class Instance:

    def __init__(self):
        self.directory = "default/"
        self.opts = {'DATABASE_URL': 'mongodb://127.0.0.1/%s_dev' % project,
                     'PORT': '3001',
                     'LOG_PATH': '/var/deploy/log/%s%s.log' % (self.directory, project)}

    def production(self):
        self.directory = "production/"
        self.opts['DATABASE_URL'] = 'mongodb://127.0.0.1/%s_production' % project
        self.opts['PORT'] = '8080'
        self.opts['PRODUCTION'] = 'true'
        self.opts['LOG_PATH'] = '/var/deploy/log/%s%s.log' % (self.directory, project)

    def staging(self):
        self.directory = "staging/"
        self.opts['PRODUCTION'] = 'true'
        self.opts['DATABASE_URL'] = 'mongodb://127.0.0.1/%s_staging' % project
        self.opts['PORT'] = '3000'
        self.opts['LOG_PATH'] = '/var/deploy/log/%s%s.log' % (self.directory, project)
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
        run("git fetch")
        run("git checkout " + branch)
        run("git pull")
        run("lein clean")
        run("lein uberjar")
        if not files.exists(deploy_location+instance.directory):
            run("mkdir %s" % deploy_location + instance.directory)
        file_name = "%s_%s.jar" % (project, datetime.today().isoformat())
        run("cp target/uberjar/%s %s" % (project+".jar", deploy_location + instance.directory + file_name))
        with cd(deploy_location + instance.directory):
            if not files.is_link(project+".jar"):
                run("mv %s.jar %s_old.jar" % project)
            run("rm %s.jar" % project)
            run("ln -s %s %s" % (file_name, project+".jar"))
    runapp()

def check():
    run("ps -ef | grep %s.jar" % project)


def runapp():
    with cd(deploy_location + instance.directory):
        if files.exists("pid"):
            pid = run("cat pid")
            with settings(warn_only=True):
                run("kill -9 " + pid.split(" ")[-1])
                run("rm pid")
        # Very powerful black magic
        run("sh -c '((%s nohup %s > /dev/null 2> /dev/null) & echo $! > pid)'" % (instance.get_opts_string(), "java -jar %s.jar" % project), pty=False)

def kill(pid):
    sudo("kill -9 " + pid)

def log(n=50, file=project+".log"):
    run("ls /var/deploy/log")
    run("tail -n %i /var/deploy/log/%s%s" % (n, instance.directory, file))

def test():
    run("echo " + instance.directory)
    run(instance.get_opts_string() + " sh -c 'echo $LOG_PATH $TEST'")
