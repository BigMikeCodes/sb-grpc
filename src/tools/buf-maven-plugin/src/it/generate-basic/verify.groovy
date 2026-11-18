def generated = new File(basedir, 'target/generated-sources/buf/dev/michaelfarrant/it/example/v1/Greeting.java')
assert generated.isFile() : "Expected generated source ${generated}"

def compiled = new File(basedir, 'target/classes/dev/michaelfarrant/it/example/v1/Greeting.class')
assert compiled.isFile() : "Expected generated source to be compiled to ${compiled}"

def installed = new File(basedir, 'target/buf').listFiles()?.findAll { it.name.startsWith('buf-') }
assert installed && installed.every { it.canExecute() } : 'Expected the resolved buf binary to be installed and executable under target/buf'
