# Adds the LiftlyWidgets extension target (Live Activity, home-screen widgets) to iosApp.xcodeproj.
# Idempotent: does nothing if the target exists. Run from the repo root: ruby tools/ios/add_widget_target.rb
require 'xcodeproj'

project_path = File.join(__dir__, '..', '..', 'iosApp', 'iosApp.xcodeproj')
project = Xcodeproj::Project.open(project_path)

app = project.targets.find { |t| t.name == 'iosApp' }
existing = project.targets.find { |t| t.name == 'LiftlyWidgets' }

# Adds a source file to a group and targets unless the group already has it (keeps the script idempotent).
def ensure_source(group, name, targets)
  return if group.files.any? { |f| f.path == name }
  ref = group.new_reference(name)
  targets.each { |t| t.add_file_references([ref]) }
end

if existing.nil?
team = app.build_configurations.first.build_settings['DEVELOPMENT_TEAM']

widget = project.new_target(:app_extension, 'LiftlyWidgets', :ios, '16.1')
widget.build_configurations.each do |config|
  s = config.build_settings
  s['PRODUCT_BUNDLE_IDENTIFIER'] = '${BUNDLE_ID}.widgets'
  s['PRODUCT_NAME'] = '$(TARGET_NAME)'
  s['INFOPLIST_FILE'] = 'LiftlyWidgets/Info.plist'
  s['GENERATE_INFOPLIST_FILE'] = 'YES'
  s['INFOPLIST_KEY_CFBundleDisplayName'] = 'Liftly'
  s['MARKETING_VERSION'] = '1.0.0'
  s['CURRENT_PROJECT_VERSION'] = '1'
  s['SWIFT_VERSION'] = '5.0'
  s['TARGETED_DEVICE_FAMILY'] = '1,2'
  s['CODE_SIGN_STYLE'] = 'Automatic'
  s['CODE_SIGN_IDENTITY'] = 'Apple Development'
  s['DEVELOPMENT_TEAM'] = team if team
  s['SKIP_INSTALL'] = 'YES'
  s['LD_RUNPATH_SEARCH_PATHS'] = ['$(inherited)', '@executable_path/Frameworks', '@executable_path/../../Frameworks']
  config.base_configuration_reference = app.build_configurations.first.base_configuration_reference
end

# Groups and sources.
shared_group = project.main_group.find_subpath('Shared', true)
shared_group.set_source_tree('<group>')
shared_group.set_path('Shared')
widget_group = project.main_group.find_subpath('LiftlyWidgets', true)
widget_group.set_source_tree('<group>')
widget_group.set_path('LiftlyWidgets')
app_group = project.main_group.find_subpath('iosApp')

attributes = shared_group.new_reference('WorkoutActivityAttributes.swift')
widget.add_file_references([attributes])
app.add_file_references([attributes])

%w[LiftlyWidgetsBundle.swift WorkoutLiveActivity.swift].each do |name|
  widget.add_file_references([widget_group.new_reference(name)])
end
widget_group.new_reference('Info.plist')
app.add_file_references([app_group.new_reference('LiveActivityIndicator.swift')])

%w[WidgetKit SwiftUI].each { |fw| widget.add_system_framework(fw) }

# Embed the extension in the app.
app.add_dependency(widget)
embed = app.new_copy_files_build_phase('Embed Foundation Extensions')
embed.symbol_dst_subfolder_spec = :plug_ins
build_file = embed.add_file_reference(widget.product_reference, true)
build_file.settings = { 'ATTRIBUTES' => ['RemoveHeadersOnCopy'] }

end

# Milestone 18: week widget + App Group shared by the app and the extension.
widget = project.targets.find { |t| t.name == 'LiftlyWidgets' }
shared = project.main_group.find_subpath('Shared')
ensure_source(shared, 'WidgetShared.swift', [app, widget])
ensure_source(project.main_group.find_subpath('LiftlyWidgets'), 'WeekWidget.swift', [widget])
ensure_source(project.main_group.find_subpath('iosApp'), 'HomeWidgetBridge.swift', [app])
{ app => 'iosApp/iosApp.entitlements', widget => 'LiftlyWidgets/LiftlyWidgets.entitlements' }.each do |target, path|
  target.build_configurations.each { |c| c.build_settings['CODE_SIGN_ENTITLEMENTS'] = path }
end

project.save
puts 'LiftlyWidgets up to date'
