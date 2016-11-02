require 'sinatra'
require 'sinatra/base'

require './generic_indexer'


class Web < Sinatra::Base




  get '/*.css' do
    response['Content-type'] = 'text/css'
    File.read(File.join('static', "#{params['splat'].first}.css"))
  end

  get '/globals.js' do
    response['Content-type'] = 'text/javascript'
    "var globals = {env: {SERVER: '#{ENV['TIMBUCTOO_BROWSER_URL']}'}};"
  end

  get '/*.js' do
    response['Content-type'] = 'text/javascript'
    File.read(File.join('static', "#{params['splat'].first}.js"))
  end

  get '/*.svg' do
    response['Content-type'] = 'image/svg+xml'
    File.read(File.join('static', "#{params['splat'].first}.svg"))
  end

  get '/fonts/*' do
    File.read(File.join('static', params['splat'].first))
  end

  get '/*' do
    File.read(File.join('static', 'index.html'))
  end

  post '/:vre_id' do
    begin
      GenericIndexer.new(
          :vre_id => params[:vre_id],
          :timbuctoo_url => ENV['TIMBUCTOO_SCRAPE_URL'],
          :solr_url => ENV['SOLR_URL']
      ).run
      status 200
    rescue Exception => e
      status 500
      raise e
    end
  end
end