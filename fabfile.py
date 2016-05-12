from fabric.api import env, run, settings, sudo, cd, local
from fabric.context_managers import cd
from fabric.contrib import files
from datetime import datetime
from StringIO import StringIO


env.hosts = "23.253.248.253:22"
env.user = "deploy"
env.forward_agent = True

project = "misabogados-workflow"
repo =  "git@bitbucket.org:constantineche/misabogados-workflow.git"
deploy_location = "/var/deploy/"

class Instance:

    websites = {'cl':
                {'psp': 'webpay',
                 'port': 0,
                 'directory': 'cl/'},
                'mx':
                {'psp': 'payu',
                 'port': 1,
                 'directory': 'mx/'},
                'co':
                {'psp': 'payu',
                 'port': 2,
                 'directory': 'co/'}}

    def __init__(self):
        self.directory = "default/"
        self.opts = {'DATABASE_URL': 'mongodb://127.0.0.1/%s_dev' % project,
                     'PORT': '3001',
                     'PAYMENT_SYSTEM': 'webpay',
                     'CURRENCY': 'CLP'}

    def production(self, website):
        self.directory = "production/" + Instance.websites[website]['directory']
        self.opts['DATABASE_URL'] = 'mongodb://127.0.0.1/%s_%s_production' % (project, website)
        self.opts['SETTINGS_DATABASE_URL'] = 'mongodb://127.0.0.1/%s_production_settings' % project
        self.opts['PORT'] = str(8080 + Instance.websites[website]['port'])
        self.opts['COUNTRY'] = website
        self.opts['PRODUCTION'] = 'true'
        self.opts['PAYMENT_SYSTEM'] = Instance.websites[website]['psp']
        self.opts['UPLOADS_URL'] = self.directory
        self.opts['UPLOADS_PATH'] = "/var/deploy/uploads/" + self.directory
        self.opts['LOG_CONFIG'] = "/var/deploy/" + self.directory + "log4j.properties"
        self.log_path = '/var/deploy/log/%s%s.log' % (self.directory, project)


    def staging(self, website):
        self.directory = "staging/" + Instance.websites[website]['directory']
        self.opts['PRODUCTION'] = 'true'
        self.opts['DATABASE_URL'] = 'mongodb://127.0.0.1/%s_%s_staging' % (project, website)
        self.opts['SETTINGS_DATABASE_URL'] = 'mongodb://127.0.0.1/%s_staging_settings' % project
        self.opts['PORT'] = str(3000 + Instance.websites[website]['port'])
        self.opts['COUNTRY'] = website
        self.opts['PAYMENT_SYSTEM'] = Instance.websites[website]['psp']
        self.opts['UPLOADS_URL'] = self.directory
        self.opts['UPLOADS_PATH'] = "/var/deploy/uploads/" + self.directory
        self.opts['LOG_CONFIG'] = "/var/deploy/" + self.directory + "log4j.properties"
        self.log_path = '/var/deploy/log/%s%s.log' % (self.directory, project)


    def get_opts_string(self):
        return ' '.join(map(lambda (k, v): k+'='+v, self.opts.iteritems()))

instance = Instance()



#============ Tasks =============

def production(website = 'cl'):
    instance.production(website)

def staging(website = 'cl'):
    instance.staging(website)


def deploy(branch="master"):
    local("ssh-agent")
    local("ssh-add")
    with cd(deploy_location):
        run("ls")
        if not files.exists(deploy_location + project):
            run("git clone " + repo)
    with cd(deploy_location + project):
        run("git fetch")
        run("git checkout " + branch)
        run("git pull")
        run("lein clean")
        with settings(warn_only=True):
            run("lein scss :production once")
        run("lein uberjar")
        if not files.exists(deploy_location+instance.directory):
            run("mkdir -p %s" % deploy_location + instance.directory)
        if not files.exists(instance.log_path):
            sudo("mkdir -p %s" % "/var/deploy/log/" + instance.directory)
            sudo("chown -R deploy:deploy %s" % "/var/deploy/log/")
        file_name = "%s_%s.jar" % (project, datetime.today().isoformat())
        run("cp target/uberjar/%s %s" % (project+".jar", deploy_location + instance.directory + file_name))
        with cd(deploy_location + instance.directory):
            if files.exists(project+".jar"):
                if not files.is_link(project+".jar"):
                    run("mv %s.jar %s_old.jar" % (project, project))
                run("rm %s.jar" % project)
            run("ln -s %s %s" % (file_name, project+".jar"))
            run("cp /var/deploy/log4j-template.properties %s" % instance.opts['LOG_CONFIG'])
            run("sed -i.bak -e 's/<###LOG_PATH###>/%s/g' %s" % (instance.log_path.replace('/', '\/'), instance.opts['LOG_CONFIG']))
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
