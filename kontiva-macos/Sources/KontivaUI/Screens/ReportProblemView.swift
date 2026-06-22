import SwiftUI
import AppKit
import KontivaCore

struct ReportProblemView: View {
    @EnvironmentObject private var model: AppModel
    @EnvironmentObject private var loc: Localizer

    @State private var summary = ""
    @State private var expected = ""
    @State private var actual = ""
    @State private var steps = ""
    @State private var area: AppSection = .overview
    @State private var composed: String?

    var body: some View {
        ScreenScroll {
            KontivaCard {
                VStack(alignment: .leading, spacing: KontivaTheme.Space.md) {
                    VStack(alignment: .leading, spacing: KontivaTheme.Space.xxs) {
                        Text(loc(.reportArea)).font(.system(size: 12, weight: .medium))
                            .foregroundStyle(KontivaTheme.textSecondary)
                        Picker(loc(.reportArea), selection: $area) {
                            ForEach(AppSection.allCases) { section in
                                Text(loc(section.titleKey)).tag(section)
                            }
                        }
                        .labelsHidden().frame(maxWidth: 280)
                    }
                    field(loc(.reportSummary), text: $summary)
                    field(loc(.reportExpected), text: $expected)
                    field(loc(.reportActual), text: $actual)
                    field(loc(.reportSteps), text: $steps, lines: 4)

                    HStack {
                        Button(loc(.commonDone)) { compose() }
                            .buttonStyle(.borderedProminent).tint(KontivaTheme.accent)
                        Spacer()
                        Text(loc(.reportCopyHint))
                            .font(.caption2).foregroundStyle(KontivaTheme.textTertiary)
                    }
                }
            }

            if let composed {
                KontivaCard {
                    VStack(alignment: .leading, spacing: KontivaTheme.Space.sm) {
                        CardTitle(loc(.reportTitle))
                        ScrollView {
                            Text(composed)
                                .font(.system(size: 12, design: .monospaced))
                                .textSelection(.enabled)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .frame(maxHeight: 260)
                        Button {
                            NSPasteboard.general.clearContents()
                            NSPasteboard.general.setString(composed, forType: .string)
                        } label: {
                            Label(loc(.commonCopy), systemImage: "doc.on.doc")
                        }
                    }
                }
            }
        }
        .navigationTitle(loc(.reportTitle))
        .navigationSubtitle(loc(.reportRedactionNote))
    }

    private func field(_ label: String, text: Binding<String>, lines: Int = 2) -> some View {
        VStack(alignment: .leading, spacing: KontivaTheme.Space.xxs) {
            Text(label).font(.system(size: 12, weight: .medium))
                .foregroundStyle(KontivaTheme.textSecondary)
            TextEditor(text: text)
                .font(.system(size: 13))
                .frame(minHeight: CGFloat(lines) * 22)
                .padding(KontivaTheme.Space.xs)
                .overlay(RoundedRectangle(cornerRadius: KontivaTheme.Radius.control)
                    .strokeBorder(KontivaTheme.softBorder, lineWidth: 1))
        }
    }

    private func compose() {
        let report = BugReport(
            summary: summary, expectedBehavior: expected,
            actualBehavior: actual, reproductionSteps: steps,
            appVersion: AppInfo.version,
            macOSVersion: AppInfo.macOSVersion,
            appLanguage: model.settings.language.rawValue,
            selectedArea: loc(area.titleKey))
        composed = report.composeRedacted()
    }
}

enum AppInfo {
    static var version: String {
        (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String) ?? "0.0.1"
    }
    static var macOSVersion: String {
        let v = ProcessInfo.processInfo.operatingSystemVersion
        return "\(v.majorVersion).\(v.minorVersion).\(v.patchVersion)"
    }
}
