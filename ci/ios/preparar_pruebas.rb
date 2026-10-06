require 'xcodeproj'

project_path = 'iosApp/iosApp.xcodeproj'
project = Xcodeproj::Project.open(project_path)
app = project.targets.find { |target| target.name == 'iosApp' }
raise 'No se encontro el target iosApp' unless app
tests = project.new_target(:ui_test_bundle, 'Sesion09UITests', :ios, '18.2')
tests.add_dependency(app)
group = project.main_group.new_group('Sesion09UITests', '../ci/ios')
file = group.new_file('CapacidadesNativasUITests.swift')
tests.add_file_references([file])
tests.build_configurations.each do |configuration|
  configuration.build_settings.merge!({
    'PRODUCT_BUNDLE_IDENTIFIER' => 'pe.edu.upeu.pharmamobil.Sesion09UITests',
    'PRODUCT_NAME' => 'Sesion09UITests',
    'PRODUCT_MODULE_NAME' => 'Sesion09UITests',
    'SWIFT_VERSION' => '5.0',
    'GENERATE_INFOPLIST_FILE' => 'YES',
    'CODE_SIGNING_ALLOWED' => 'NO',
    'TEST_TARGET_NAME' => 'iosApp',
    'IPHONEOS_DEPLOYMENT_TARGET' => '18.2',
    'TARGETED_DEVICE_FAMILY' => '1,2'
  })
end
project.save
scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(app)
scheme.add_build_target(tests)
scheme.add_test_target(tests)
scheme.set_launch_target(app)
scheme.save_as(project_path, 'Sesion09', true)
puts 'Target de pruebas generado solo para la verificacion en macOS'
