const gulp = require("gulp");
const sourcemaps = require("gulp-sourcemaps");
const md5 = require("gulp-md5-plus");
const merge = require("merge2");

var argv = require('yargs').argv;
var root = `${argv.root}/src/main`;

gulp.task("clean", function() {
    const del = require("del");
    return del(`${root}/dist/web`, {force: true});
});

gulp.task("html", function() {
    return gulp.src(`${root}/web/template/**/*.html`)
        .pipe(gulp.dest(`${root}/dist/web/template`))
});

gulp.task("resource", function() {
    return gulp.src([`${root}/web/**/*.*`, `!${root}/web/static/css/**/*.css`, `!${root}/web/static/js/**/*.js`, `!${root}/web/template/**/*.*`])
        .pipe(gulp.dest(`${root}/dist/web`));
});

gulp.task("css", ["html"], function() {
    const stylelint = require("gulp-stylelint");
    const cssnano = require("gulp-cssnano");

    var appCSS = gulp.src([`${root}/web/static/css/**/*.css`, `!${root}/web/static/css/lib{,/**/*.css}`])
        .pipe(sourcemaps.init())
        .pipe(stylelint({
            configFile: "stylelint.json",
            reporters: [{
                formatter: "string",
                console: true
            }]
        }))
        .pipe(cssnano())
        .pipe(md5(10, `${root}/dist/web/template/**/*.html`))
        .pipe(sourcemaps.write("."))
        .pipe(gulp.dest(`${root}/dist/web/static/css`));

    var libCSS = gulp.src(`${root}/web/static/css/lib/*.css`)
        .pipe(md5(10, `${root}/dist/web/template/**/*.html`))
        .pipe(gulp.dest(`${root}/dist/web/static/css/lib`));

    return merge(appCSS, libCSS);
});

gulp.task("js", ["html"], function(cb) {
    const uglify = require("gulp-uglify");
    const eslint = require("gulp-eslint");

    var appJS = gulp.src([`${root}/web/static/js/**/*.js`, `!${root}/web/static/js/lib{,/**/*.js}`])
        .pipe(eslint({
            configFile: "eslint.json"
        }))
        .pipe(eslint.format())
        .pipe(eslint.failAfterError())
        .pipe(sourcemaps.init())
        .pipe(uglify())
        .pipe(md5(10, `${root}/dist/web/template/**/*.html`))
        .pipe(sourcemaps.write("."))
        .pipe(gulp.dest(`${root}/dist/web/static/js`));

    var libJS = gulp.src([`${root}/web/static/js/lib/**/*.js`])
        .pipe(md5(10, `${root}/dist/web/template/**/*.html`))
        .pipe(gulp.dest(`${root}/dist/web/static/js/lib`));

    return merge(appJS, libJS);
});

gulp.task("build", [], function() {
    gulp.start("resource", "html", "css", "js");
});