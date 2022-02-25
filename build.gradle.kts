allprojects {
    repositories {
        mavenCentral()
    }

    pluginManager.withPlugin("java") {
        val isModuleProject = file("src/main/java/module-info.java").exists()

        configure<JavaPluginExtension> {
            modularity.inferModulePath.set(false)
        }

        val compileClasspath by configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME)

        val moduleCompileClasspath = configurations.create("moduleCompileClasspath") {
            extendsFrom(compileClasspath)
            isCanBeConsumed = false // We don't want this configuration used by dependent projects
            attributes {
                // Prefer compiling against the classes dir if available vs the jar
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
            }
        }.incoming.artifactView { // Construct an artifact view that contains only those dependencies we want on the module path
            val moduleComponents by lazy {
                walkResolvedComponent(project, compileClasspath.incoming.resolutionResult.root, isModuleProject)
            }
            componentFilter {
                moduleComponents.contains(it)
            }
        }.files

        tasks {
            named<JavaCompile>("compileJava") {
                val argumentProvider = CompileModulePathArgumentProvider(isModuleProject, moduleCompileClasspath)
                options.compilerArgumentProviders.add(argumentProvider)
                classpath = classpath.minus(moduleCompileClasspath) // Classpath is just everything not also on the module path

                doFirst {
                    println("Module path args: ${argumentProvider.asArguments()}")
                    println("Classpath: ${classpath.asPath}")
                }
            }
        }
    }
}

class CompileModulePathArgumentProvider(@get:Input val isModuleProject: Boolean, @get:CompileClasspath val modulePath: FileCollection) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> {
        val args = mutableListOf<String>()
        if (!modulePath.isEmpty) {
            if (!isModuleProject) {
                args.add("--add-modules=ALL-MODULE-PATH")
            }
            args.add("--module-path=${modulePath.asPath}")
        }

        return args
    }
}

fun walkResolvedComponent(project: Project, result: ResolvedComponentResult, isModuleDependency: Boolean): List<ComponentIdentifier> {
    return result.dependencies.filterIsInstance<ResolvedDependencyResult>()
        .filter {
            val id = it.selected.id
            // Include dependency if we are a descendent of another module dependency or this is project dependency with a module-info file
            isModuleDependency || (id is ProjectComponentIdentifier && project.findProject(id.projectPath)!!.file("src/main/java/module-info.java").exists())
        }.flatMap {
            walkResolvedComponent(project, it.selected, true).plusElement(it.selected.id)
        }
}