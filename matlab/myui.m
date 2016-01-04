 function myui(data)
    if ischar(data)
        if strcmp(data(end-6:end),'001.tif')
            base = data(1:end-7);
            data = {};
            
            ind = 1;
            n = '001';
            fname = [base,n,'.tif'];
            while exist(fname,'file') == 2
                data{ind} = fname;
                ind = ind+1;
                n = num2str(ind);
                while length(n) < 3
                    n = ['0',n];
                end
                fname = [base,n,'.tif'];
            end
            [im, n_images] = load_series(data);
            disp(['Found ', num2str(n_images), ' images.'])
        else
            
            stack = data;
            info = imfinfo(stack);
            n_images = numel(info);
            H = info(1).Height;
            W = info(1).Width;

            im = zeros(H,W,n_images);
            for k = 1:n_images
                im(:,:,k) = mat2gray(imread(stack,k));
            end
        end
    elseif iscell(data)
        [im, n_images] = load_series(data);
    else
        error('Unable to understand input.')
    end
    
 

    
    % Create a figure and axes
    f = figure('Visible','off');
    ax = axes('Units','pixels');
    %imagesc(im(:,:,1))
    
    % Create pop-up menu
    popup = uicontrol('Style', 'popup',...
           'String', {'parula','jet','hsv','hot','cool','gray'},...
           'Position', [20 340 100 50],...
           'Callback', @setmap);    

    % Create pop-up menu
    popup_filter = uicontrol('Style', 'popup',...
           'String', {'gaussian','LoG'},...
           'Position', [20 300 100 50],...
           'Callback', @setfilter);        
       
   % Create push button
    btn = uicontrol('Style', 'togglebutton', 'String', 'Superimpose',...
        'Position', [20 20 150 20],...
        'Callback', @plot_things);       

   % Create slider for frames
    sld_frames = uicontrol('Style', 'slider',...
        'Min',1,'Max',n_images,'Value',1,'SliderStep',[1/(n_images-1) , 10/(n_images-1) ],...
        'Position', [400 20 120 20],...
        'Callback', @setslice); 

   % Create slider
    sld_sigma = uicontrol('Style', 'slider',...
        'Min',0.5,'Max',20,'Value',9,'SliderStep',[1/(39) , 10/(39) ],...
        'Position', [400 60 120 20],...
        'Callback', @setsigma); 
    
    
    % Add a text uicontrol to label the slider.
    txt_sigma = uicontrol('Style','text',...
        'Position',[400 45 120 20],...
        'String','sigma = 9');
    
    % Add a text uicontrol to label the slider.
    txt_frames = uicontrol('Style','text',...
        'Position',[400 5 120 20],...
        'String','frame # 1');


   % Create checkbox
    chkbox = uicontrol('Style', 'checkbox',...
        'Position', [20 280 15 15],...
        'Callback', @plot_things); 
    
    
    % Add a text uicontrol to label the slider.
    txt_chkbox = uicontrol('Style','text',...
        'Position',[37.5 284 50 10],...
        'HorizontalAlignment','left',...
        'String','Invert filter');    
    
    
    plot_things()
    % Make figure visble after adding all components
    f.Visible = 'on';
    % This code uses dot notation to set properties. 
    % Dot notation runs in R2014b and later.
    % For R2014a and earlier: set(f,'Visible','on');
    
    
    
    function setmap(source,callbackdata)
        val = source.Value;
        maps = source.String;
        % For R2014a and earlier: 
        % val = get(source,'Value');
        % maps = get(source,'String'); 

        newmap = maps{val};
        colormap(newmap);
    end

    function setslice(source,callbackdata)
        
        plot_things()
%         subplot(2,1,1)
%         frame = int32(get(source,'Value'));
%         imagesc(im(:,:,frame))
%         axis image
%         imf = imfilter(im(:,:,frame),-fspecial('log',[30,30],9),'symmetric');
%         subplot(2,1,2)
%         imagesc(imf)
%         axis image
    end

     function setsigma(source,callbackdata)
        
        plot_things()
%         subplot(2,1,1)
%         frame = int32(get(source,'Value'));
%         imagesc(im(:,:,frame))
%         axis image
%         imf = imfilter(im(:,:,frame),-fspecial('log',[30,30],9),'symmetric');
%         subplot(2,1,2)
%         imagesc(imf)
%         axis image       

        
     end

     function setfilter(source,callbackdata)
         plot_things()
     end
 
     function plot_things(varargin)
        subplot(2,1,1)
        frame = int32(get(sld_frames,'Value'));
        sigma = get(sld_sigma,'Value');
        imagesc(im(:,:,frame))
        axis image
        colorbar()
        plot_type = get(btn,'Value');
        
        
        filter_type = get(popup_filter,'Value');
        hbox = max(30,round(sigma*2.5));
        sign = (-1)^get(chkbox,'Value');
        
        
        switch filter_type
            case 1
                filter = sign*fspecial('gaussian',[hbox,hbox],sigma);
            case 2
                filter = sign*(-1)*fspecial('log',[hbox,hbox],sigma);
        end
        imf = imfilter(im(:,:,frame),filter,'symmetric');
        subplot(2,1,2)
        if plot_type == 0
            imagesc(imf)
        else
            imshowpair(im(:,:,frame),imf)
        end
        axis image       
        colorbar()
        set(txt_sigma,'String',['sigma = ', num2str(sigma)])
        set(txt_frames,'String',['frame # ', num2str(frame), '/', num2str(n_images)])
     end
 
     function [im,n_images] = load_series(data)
        info = imfinfo(data{1});
        n_images = numel(data);
        H = info(1).Height;
        W = info(1).Width;
        
        im = zeros(H,W,n_images);
        for k = 1:n_images
            im(:,:,k) = mat2gray(imread(data{k}));
        end
        
     end
end